package net.exmo.exphone;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** -1 requests a voice note; negative total marks a download. */
public record PhoneAudioPayload(String id, int index, int total, byte[] bytes) implements CustomPacketPayload {
    static final int CHUNK = 16 * 1024;
    static final int MAX_BYTES = 2 * 1024 * 1024;
    static final Type<PhoneAudioPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExPhone.MODID, "phone_audio_v2"));
    static final StreamCodec<RegistryFriendlyByteBuf, PhoneAudioPayload> CODEC = new StreamCodec<>() {
        public PhoneAudioPayload decode(RegistryFriendlyByteBuf b) { return new PhoneAudioPayload(b.readUtf(64),b.readVarInt(),b.readVarInt(),b.readByteArray(CHUNK)); }
        public void encode(RegistryFriendlyByteBuf b,PhoneAudioPayload p) { b.writeUtf(p.id,64);b.writeVarInt(p.index);b.writeVarInt(p.total);b.writeByteArray(p.bytes); }
    };
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
