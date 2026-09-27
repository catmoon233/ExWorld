package net.exmo.exphone;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Captures SVC microphone PCM without opening a competing audio device. */
final class PhoneRecorder {
    private static final AudioFormat FORMAT = new AudioFormat(48_000, 16, 1, true, false);
    private static final int MAX_BYTES = 48_000 * 2 * 15;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "phone-recorder-io");t.setDaemon(true);return t; });
    private static final List<String> clips = new ArrayList<>();
    private static Path directory;
    private static ByteArrayOutputStream recording;
    private static String recordId;
    private static Clip playing;
    private static String uploadId="",pendingPlay="";
    private static Runnable afterUpload;
    private static final Map<String,ByteArrayOutputStream> incoming=new HashMap<>();
    private static final Map<String,Integer> receivedNext=new HashMap<>();

    static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String world = mc.hasSingleplayerServer()
                ? mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().toString()
                : mc.getCurrentServer() == null ? "unknown" : mc.getCurrentServer().ip;
        String scope = UUID.nameUUIDFromBytes(world.getBytes(StandardCharsets.UTF_8)).toString();
        Path next = mc.gameDirectory.toPath().resolve("phone-recordings").resolve(scope).resolve(mc.getUser().getProfileId().toString());
        if (next.equals(directory)) return;
        reset();directory=next;
        IO.execute(() -> {
            List<String> found = new ArrayList<>();
            try {
                Files.createDirectories(next);
                try (var files=Files.list(next)) {
                    files.filter(p->p.getFileName().toString().endsWith(".wav"))
                            .sorted(Comparator.comparingLong((Path p)->p.toFile().lastModified()).reversed())
                            .forEach(p->found.add(p.getFileName().toString().replace(".wav","")));
                }
            } catch (Exception ignored) { }
            mc.execute(()->{if(next.equals(directory)){clips.clear();clips.addAll(found);PhoneApricity.albumChanged();}});
        });
    }
    static List<String> clips(){open();return List.copyOf(clips);}
    static synchronized boolean recording(){return recording!=null;}
    static synchronized String start(){
        open();
        if(!PhoneCalls.installed())return "请安装并连接 Simple Voice Chat";
        if(recording!=null)return "正在录音";
        if(directory==null)return "请先进入世界";
        recording=new ByteArrayOutputStream();recordId=UUID.randomUUID().toString();return "开始录音 · 请在语音模组中启用麦克风";
    }
    static synchronized void capture(short[] samples){
        if(recording==null||samples==null)return;
        if(recording.size()+samples.length*2>MAX_BYTES){Minecraft.getInstance().execute(PhoneRecorder::stop);return;}
        for(short sample:samples){recording.write(sample&255);recording.write(sample>>>8&255);}
    }
    static synchronized String stop(){
        if(recording==null)return "尚未开始录音";
        byte[] pcm=recording.toByteArray();String id=recordId;Path path=directory;
        recording=null;recordId=null;
        if(pcm.length==0)return "没有收到语音数据；请检查 SVC 连接和按键说话设置";
        IO.execute(()->{
            try {
                Files.createDirectories(path);
                try(var input=new AudioInputStream(new ByteArrayInputStream(pcm),FORMAT,pcm.length/2L)){
                    AudioSystem.write(input,AudioFileFormat.Type.WAVE,path.resolve(id+".wav").toFile());
                }
                Minecraft.getInstance().execute(()->{if(path.equals(directory)){clips.addFirst(id);PhoneApricity.notice("录音已保存");PhoneApricity.albumChanged();}});
            } catch(Exception error){Minecraft.getInstance().execute(()->PhoneApricity.notice("录音保存失败："+error.getMessage()));}
        });
        return "录音保存中";
    }
    static void play(String id){
        open();
        if(!PhonePhotos.validId(id)||directory==null)return;
        Path local=directory.resolve(id+".wav"),remote=directory.resolve("remote-"+id+".wav");
        if(!Files.isRegularFile(local)&&!Files.isRegularFile(remote)){
            pendingPlay=id;PacketDistributor.sendToServer(new PhoneAudioPayload(id,-1,0,new byte[0]));
            PhoneApricity.notice("正在下载语音");return;
        }
        Path file=Files.isRegularFile(local)?local:remote;
        IO.execute(()->{
            try {
                if(playing!=null){playing.stop();playing.close();}
                Clip clip=AudioSystem.getClip();
                try(var audio=AudioSystem.getAudioInputStream(file.toFile())){clip.open(audio);}
                playing=clip;clip.start();
            }catch(Exception error){Minecraft.getInstance().execute(()->PhoneApricity.notice("播放失败："+error.getMessage()));}
        });
    }
    static void upload(String id,Runnable onSuccess){
        open();if(!uploadId.isBlank()){PhoneApricity.notice("上一段语音仍在上传");return;}
        if(!PhonePhotos.validId(id)||directory==null||!Files.isRegularFile(directory.resolve(id+".wav"))){PhoneApricity.notice("找不到录音");return;}
        uploadId=id;afterUpload=onSuccess;Path file=directory.resolve(id+".wav");
        IO.execute(()->{
            try {
                byte[] bytes=Files.readAllBytes(file);
                if(bytes.length<44||bytes.length>PhoneAudioPayload.MAX_BYTES)throw new IllegalArgumentException("语音大小无效");
                Minecraft.getInstance().execute(()->{
                    int total=(bytes.length+PhoneAudioPayload.CHUNK-1)/PhoneAudioPayload.CHUNK;
                    for(int i=0;i<total;i++)PacketDistributor.sendToServer(new PhoneAudioPayload(id,i,total,
                            Arrays.copyOfRange(bytes,i*PhoneAudioPayload.CHUNK,Math.min(bytes.length,(i+1)*PhoneAudioPayload.CHUNK))));
                });
            }catch(Exception error){Minecraft.getInstance().execute(()->PhoneApricity.notice("语音上传失败："+error.getMessage()));}
        });
    }
    static void uploaded(JsonObject value){
        String id=PhoneApricity.str(value,"id");if(!id.equals(uploadId))return;
        uploadId="";Runnable callback=afterUpload;afterUpload=null;
        if(PhoneApricity.bool(value,"ok")){if(callback!=null)callback.run();}
        else PhoneApricity.notice(PhoneApricity.str(value,"message"));
    }
    static void receive(PhoneAudioPayload packet){
        if(!PhonePhotos.validId(packet.id())||packet.total()>=0||packet.index()<0)return;
        int total=-packet.total();
        if(total<1||total>(PhoneAudioPayload.MAX_BYTES+PhoneAudioPayload.CHUNK-1)/PhoneAudioPayload.CHUNK)return;
        if(packet.index()==0){incoming.put(packet.id(),new ByteArrayOutputStream());receivedNext.put(packet.id(),0);}
        ByteArrayOutputStream bytes=incoming.get(packet.id());
        if(bytes==null||receivedNext.getOrDefault(packet.id(),-1)!=packet.index()||bytes.size()+packet.bytes().length>PhoneAudioPayload.MAX_BYTES){incoming.remove(packet.id());receivedNext.remove(packet.id());return;}
        bytes.writeBytes(packet.bytes());receivedNext.put(packet.id(),packet.index()+1);
        if(packet.index()+1<total)return;
        incoming.remove(packet.id());receivedNext.remove(packet.id());
        Path path=directory;if(path==null)return;byte[] content=bytes.toByteArray();String id=packet.id();
        IO.execute(()->{
            try {
                Files.createDirectories(path);Files.write(path.resolve("remote-"+id+".wav"),content);
                Minecraft.getInstance().execute(()->{if(path.equals(directory)&&id.equals(pendingPlay)){pendingPlay="";play(id);}});
            }catch(Exception error){Minecraft.getInstance().execute(()->PhoneApricity.notice("语音下载失败"));}
        });
    }
    static void delete(String id){
        if(!PhonePhotos.validId(id)||directory==null)return;
        Path path=directory;IO.execute(()->{try{Files.deleteIfExists(path.resolve(id+".wav"));}catch(Exception ignored){}
            Minecraft.getInstance().execute(()->{if(path.equals(directory)){clips.remove(id);PhoneApricity.albumChanged();}});
        });
    }
    static void reset(){stop();if(playing!=null){playing.stop();playing.close();playing=null;}clips.clear();directory=null;uploadId="";afterUpload=null;pendingPlay="";incoming.clear();receivedNext.clear();}
}
