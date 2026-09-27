package net.exmo.exphone;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Index -1 requests a thumbnail; -2 requests the full photograph. */
public record PhonePhotoPayload(String id, int index, int total, byte[] bytes) implements CustomPacketPayload {
    public static final int CHUNK = 16 * 1024;
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final Type<PhonePhotoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExPhone.MODID, "phone_photo_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhonePhotoPayload> CODEC = new StreamCodec<>() {
        public PhonePhotoPayload decode(RegistryFriendlyByteBuf b) {
            return new PhonePhotoPayload(b.readUtf(64), b.readVarInt(), b.readVarInt(), b.readByteArray(CHUNK));
        }
        public void encode(RegistryFriendlyByteBuf b, PhonePhotoPayload p) {
            b.writeUtf(p.id, 64); b.writeVarInt(p.index); b.writeVarInt(p.total); b.writeByteArray(p.bytes);
        }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
