package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Opens the nearby-chat configuration screen with the server's current distance and range. */
public record NearbyChatOpenPayload(int distance, int range) implements CustomPacketPayload {
    public static final Type<NearbyChatOpenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "nearby_chat_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NearbyChatOpenPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.distance);
                buffer.writeVarInt(payload.range);
            },
            buffer -> new NearbyChatOpenPayload(buffer.readVarInt(), buffer.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
