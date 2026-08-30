package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MonsterPackageActionPayload(Action action,String packageId,int index,String fileName,String templatesJson,String rulesJson) implements CustomPacketPayload {
    public enum Action { REQUEST_LIST, ENABLE, DISABLE, MOVE, RELOAD, IMPORT, EXPORT, OPEN_EDITOR, SAVE }
    public static final Type<MonsterPackageActionPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID,"monster_package_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MonsterPackageActionPayload> STREAM_CODEC=StreamCodec.of(MonsterPackageActionPayload::encode,MonsterPackageActionPayload::decode);
    public MonsterPackageActionPayload{packageId=packageId==null?"":packageId;fileName=fileName==null?"":fileName;templatesJson=templatesJson==null?"[]":templatesJson;rulesJson=rulesJson==null?"[]":rulesJson;}
    private static void encode(RegistryFriendlyByteBuf b,MonsterPackageActionPayload p){b.writeEnum(p.action);b.writeUtf(p.packageId);b.writeVarInt(p.index);b.writeUtf(p.fileName);b.writeUtf(p.templatesJson);b.writeUtf(p.rulesJson);}
    private static MonsterPackageActionPayload decode(RegistryFriendlyByteBuf b){return new MonsterPackageActionPayload(b.readEnum(Action.class),b.readUtf(),b.readVarInt(),b.readUtf(),b.readUtf(1_000_000),b.readUtf(1_000_000));}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
