package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestWorldMapPayload() implements CustomPacketPayload {
    public static final Type<RequestWorldMapPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "request_world_map"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestWorldMapPayload> STREAM_CODEC = StreamCodec.unit(new RequestWorldMapPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
