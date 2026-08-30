package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestRespawnAnchorsPayload() implements CustomPacketPayload {
    public static final Type<RequestRespawnAnchorsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "request_respawn_anchors"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestRespawnAnchorsPayload> STREAM_CODEC = StreamCodec.unit(new RequestRespawnAnchorsPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
