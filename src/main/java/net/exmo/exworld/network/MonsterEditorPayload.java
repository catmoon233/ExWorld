package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Complete server projection for the editor. JSON is bounded and revalidated on save. */
public record MonsterEditorPayload(String packageId, String displayName, String version,
                                   String templatesJson, String rulesJson) implements CustomPacketPayload {
    public static final Type<MonsterEditorPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID,"monster_editor"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MonsterEditorPayload> STREAM_CODEC=StreamCodec.of(MonsterEditorPayload::encode,MonsterEditorPayload::decode);
    public MonsterEditorPayload { if (templatesJson.length()>1_000_000 || rulesJson.length()>1_000_000) throw new IllegalArgumentException("monster editor data is too large"); }
    private static void encode(RegistryFriendlyByteBuf b,MonsterEditorPayload p){b.writeUtf(p.packageId);b.writeUtf(p.displayName);b.writeUtf(p.version);b.writeUtf(p.templatesJson);b.writeUtf(p.rulesJson);}
    private static MonsterEditorPayload decode(RegistryFriendlyByteBuf b){return new MonsterEditorPayload(b.readUtf(),b.readUtf(),b.readUtf(),b.readUtf(1_000_000),b.readUtf(1_000_000));}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
