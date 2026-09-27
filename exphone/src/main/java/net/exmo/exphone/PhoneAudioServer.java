package net.exmo.exphone;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded and access-checked voice-note storage. */
final class PhoneAudioServer {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"phone-audio-io");t.setDaemon(true);return t;});
    private static final Map<UUID,Upload> uploads=new HashMap<>();
    private static final Set<UUID> busy=new HashSet<>();
    private static final long QUOTA=Math.max(1,Long.getLong("exphone.phone.audioQuotaMiB",64L))*1024*1024;
    private record Upload(String id,ByteArrayOutputStream bytes,int total,int next,long since){}

    static void receive(ServerPlayer player,PhoneAudioPayload packet){
        if(!PhonePhotos.validId(packet.id()))return;
        if(packet.index()==-1){download(player,packet.id());return;}
        if(busy.contains(player.getUUID())){reply(player,packet.id(),false,"请等待上一段语音完成");return;}
        if(packet.index()<0||!PhoneWifi.online(player)||PhoneStacks.heldPhone(player).isEmpty()){reply(player,packet.id(),false,"网络不可用");return;}
        PhoneData data=PhoneData.get(player);PhoneData.Profile self=data.ensure(player);
        if(self.number.isBlank()||!self.number.equals(self.boundNumber)){reply(player,packet.id(),false,"微信未绑定手机号");return;}
        if(data.audioOwners.containsKey(packet.id())){reply(player,packet.id(),self.id.equals(data.audioOwners.get(packet.id())),"语音已上传");return;}
        long now=System.currentTimeMillis();uploads.values().removeIf(u->now-u.since>30_000);
        int maxChunks=(PhoneAudioPayload.MAX_BYTES+PhoneAudioPayload.CHUNK-1)/PhoneAudioPayload.CHUNK;
        if(packet.total()<1||packet.total()>maxChunks||packet.bytes().length<1||packet.bytes().length>PhoneAudioPayload.CHUNK){reply(player,packet.id(),false,"语音分片无效");return;}
        Upload current=packet.index()==0?new Upload(packet.id(),new ByteArrayOutputStream(),packet.total(),0,now):uploads.get(player.getUUID());
        if(current==null||!current.id.equals(packet.id())||current.total!=packet.total()||current.next!=packet.index()){reply(player,packet.id(),false,"语音分片顺序错误");return;}
        if(current.bytes.size()+packet.bytes().length>PhoneAudioPayload.MAX_BYTES){uploads.remove(player.getUUID());reply(player,packet.id(),false,"语音太长");return;}
        current.bytes.writeBytes(packet.bytes());
        current=new Upload(current.id,current.bytes,current.total,current.next+1,current.since);
        if(current.next<current.total){uploads.put(player.getUUID(),current);return;}
        uploads.remove(player.getUUID());busy.add(player.getUUID());byte[] bytes=current.bytes.toByteArray();Path path=root(player).resolve(self.id).resolve(packet.id()+".wav");
        IO.execute(()->{
            String error="";
            try {
                validate(bytes);Files.createDirectories(path.getParent());
                long used;try(var files=Files.list(path.getParent())){used=files.mapToLong(p->{try{return Files.size(p);}catch(Exception ignored){return QUOTA;}}).sum();}
                if(used+bytes.length>QUOTA)error="录音存储额度已用完";
                else Files.write(path,bytes);
            }catch(Exception failure){error="语音格式或存储无效";}
            String result=error;
            player.server.execute(()->{
                busy.remove(player.getUUID());
                if(result.isEmpty()){data.audioOwners.put(packet.id(),self.id);data.touch();}
                if(!player.hasDisconnected())reply(player,packet.id(),result.isEmpty(),result.isEmpty()?"语音已上传":result);
            });
        });
    }
    private static void validate(byte[] bytes)throws Exception{
        if(bytes.length<44||bytes.length>PhoneAudioPayload.MAX_BYTES)throw new IllegalArgumentException();
        try(AudioInputStream input=AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes))){
            AudioFormat format=input.getFormat();
            if(!format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)||format.getChannels()!=1
                    ||format.getSampleSizeInBits()!=16||format.getSampleRate()!=48_000
                    ||input.getFrameLength()<1||input.getFrameLength()>48_000*15L)throw new IllegalArgumentException();
        }
    }
    private static void download(ServerPlayer player,String id){
        PhoneData data=PhoneData.get(player);PhoneData.Profile self=data.ensure(player);
        if(!PhoneWifi.online(player)||!allowed(data,self,id))return;
        String owner=data.audioOwners.get(id);if(owner==null)return;
        Path file=root(player).resolve(owner).resolve(id+".wav");
        IO.execute(()->{
            try {
                byte[] bytes=Files.readAllBytes(file);if(bytes.length>PhoneAudioPayload.MAX_BYTES)return;
                int total=(bytes.length+PhoneAudioPayload.CHUNK-1)/PhoneAudioPayload.CHUNK;
                player.server.execute(()->{
                    if(player.hasDisconnected()||!allowed(data,data.ensure(player),id))return;
                    for(int i=0;i<total;i++)PacketDistributor.sendToPlayer(player,new PhoneAudioPayload(id,i,-total,
                            Arrays.copyOfRange(bytes,i*PhoneAudioPayload.CHUNK,Math.min(bytes.length,(i+1)*PhoneAudioPayload.CHUNK))));
                });
            }catch(Exception ignored){}
        });
    }
    private static boolean allowed(PhoneData data,PhoneData.Profile self,String id){
        return self.id.equals(data.audioOwners.get(id))||data.conversations.stream().anyMatch(message->message.kind().equals("audio")
                &&message.extra().equals(id)&&message.visible(data,self));
    }
    private static Path root(ServerPlayer player){return player.server.getWorldPath(LevelResource.ROOT).resolve("data/lotm_phone_audio");}
    private static void reply(ServerPlayer player,String id,boolean ok,String message){
        JsonObject value=new JsonObject();value.addProperty("id",id);value.addProperty("ok",ok);value.addProperty("message",message);
        JsonObject packet=new JsonObject();packet.addProperty("version",2);packet.addProperty("section","audioTransfer");packet.add("value",value);
        PacketDistributor.sendToPlayer(player,new PhonePayloads.PhoneStatePayload(packet.toString()));
    }
}
