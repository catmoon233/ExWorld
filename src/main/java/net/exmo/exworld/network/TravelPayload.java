package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TravelPayload(String tileId) implements CustomPacketPayload {
    public static final Type<TravelPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "travel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TravelPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeUtf(payload.tileId), buffer -> new TravelPayload(buffer.readUtf()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
