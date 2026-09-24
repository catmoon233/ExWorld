package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client save of the nearby-chat distance and vertical range. The server re-checks permission. */
public record NearbyChatUpdatePayload(int distance, int range) implements CustomPacketPayload {
    public static final Type<NearbyChatUpdatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "nearby_chat_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NearbyChatUpdatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.distance);
                buffer.writeVarInt(payload.range);
            },
            buffer -> new NearbyChatUpdatePayload(buffer.readVarInt(), buffer.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
