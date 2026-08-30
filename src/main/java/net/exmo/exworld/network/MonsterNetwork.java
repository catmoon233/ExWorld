package net.exmo.exworld.network;

import net.exmo.exworld.monster.MonsterPackageManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MonsterNetwork {
    private MonsterNetwork() {}
    public static void register(PayloadRegistrar registrar){
        registrar.playToClient(MonsterEditorPayload.TYPE,MonsterEditorPayload.STREAM_CODEC,(payload,context)->net.exmo.exworld.client.monster.MonsterEditorScreen.install(payload));
        registrar.playToClient(MonsterPackageListPayload.TYPE,MonsterPackageListPayload.STREAM_CODEC,(payload,context)->net.exmo.exworld.client.monster.MonsterPackageScreen.install(payload));
        registrar.playToServer(MonsterPackageActionPayload.TYPE,MonsterPackageActionPayload.STREAM_CODEC,(payload,context)->{if(context.player() instanceof ServerPlayer player)handle(player,payload);});
    }
    public static void sendEditor(ServerPlayer player,MonsterPackageManager.LoadedPackage pack){
        var gson=new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        PacketDistributor.sendToPlayer(player,new MonsterEditorPayload(pack.info().id(),pack.info().displayName(),pack.info().version(),gson.toJson(pack.templates().values()),gson.toJson(pack.rules())));
    }
    public static void sendPackages(ServerPlayer player){var registry=net.exmo.exworld.monster.MonsterProfileRegistry.active().orElse(null);if(registry==null)return;var entries=registry.packages().stream().map(p->new MonsterPackageListPayload.Entry(p.id(),p.displayName(),p.version(),p.valid(),p.error())).toList();PacketDistributor.sendToPlayer(player,new MonsterPackageListPayload(entries,registry.enabled()));}
    private static void handle(ServerPlayer player,MonsterPackageActionPayload payload){
        if(!player.hasPermissions(2))return;var registry=net.exmo.exworld.monster.MonsterProfileRegistry.active().orElse(null);if(registry==null)return;boolean ok=true;
        try{switch(payload.action()){
            case REQUEST_LIST->{} case ENABLE->ok=registry.setEnabled(payload.packageId(),true);case DISABLE->ok=registry.setEnabled(payload.packageId(),false);case MOVE->ok=registry.reorder(payload.packageId(),payload.index());case RELOAD->ok=registry.reload();case IMPORT->registry.manager().importPackage(payload.fileName());case EXPORT->registry.manager().exportPackage(payload.packageId());case OPEN_EDITOR->{var pack=registry.manager().load(payload.packageId());sendEditor(player,pack);return;}case SAVE->ok=registry.savePackage(payload.packageId(),payload.templatesJson(),payload.rulesJson());
        }}catch(Exception error){ok=false;player.sendSystemMessage(Component.literal("怪物数据包操作失败："+error.getMessage()));}
        if(payload.action()==MonsterPackageActionPayload.Action.SAVE){try{sendEditor(player,registry.manager().load(payload.packageId()));return;}catch(Exception ignored){}}
        if(!ok)player.sendSystemMessage(Component.literal("怪物数据包操作被拒绝"));sendPackages(player);
    }
}
