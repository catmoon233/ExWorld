package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-authored decryption-mode flag. The server config is the source of truth. */
public record DecryptionModePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<DecryptionModePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "decryption_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DecryptionModePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBoolean(payload.enabled),
            buffer -> new DecryptionModePayload(buffer.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
