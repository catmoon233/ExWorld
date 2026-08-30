package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.world.model.WorldSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WorldSnapshotPayload(WorldSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<WorldSnapshotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "world_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            WorldSnapshotPayload::encode, WorldSnapshotPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, WorldSnapshotPayload payload) {
        buffer.writeByteArray(WorldSnapshotCompression.encode(payload.snapshot));
    }

    private static WorldSnapshotPayload decode(RegistryFriendlyByteBuf buffer) {
        byte[] compressed = buffer.readByteArray(WorldSnapshotCompression.MAX_COMPRESSED_BYTES);
        return new WorldSnapshotPayload(WorldSnapshotCompression.decode(compressed));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
