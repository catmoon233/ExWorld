package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.world.model.WorldSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server projection of the authoritative group editor, including an optional validation error from the last save. */
public record WorldGroupEditorPayload(WorldSnapshot snapshot, String error) implements CustomPacketPayload {
    public static final Type<WorldGroupEditorPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "world_group_editor"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldGroupEditorPayload> STREAM_CODEC = StreamCodec.of(
            WorldGroupEditorPayload::encode, WorldGroupEditorPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, WorldGroupEditorPayload payload) {
        buffer.writeByteArray(WorldSnapshotCompression.encode(payload.snapshot));
        buffer.writeUtf(payload.error, 512);
    }

    private static WorldGroupEditorPayload decode(RegistryFriendlyByteBuf buffer) {
        return new WorldGroupEditorPayload(WorldSnapshotCompression.decode(
                buffer.readByteArray(WorldSnapshotCompression.MAX_COMPRESSED_BYTES)), buffer.readUtf(512));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
