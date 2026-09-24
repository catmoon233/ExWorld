package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Creative map teleport. The server rejects anyone who is not in creative mode. */
public record MapTeleportPayload(int x, int z) implements CustomPacketPayload {
    public static final Type<MapTeleportPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "map_teleport"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MapTeleportPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeInt(payload.x); buffer.writeInt(payload.z); },
            buffer -> new MapTeleportPayload(buffer.readInt(), buffer.readInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
