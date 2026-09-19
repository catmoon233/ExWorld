package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.world.model.ChunkGroupShape;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Small clientbound shape update sent only when the player enters another region. */
public record ActiveChunkGroupPayload(ChunkGroupShape shape, boolean archipelago) implements CustomPacketPayload {
    public static final Type<ActiveChunkGroupPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "active_chunk_group"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActiveChunkGroupPayload> STREAM_CODEC = StreamCodec.of(
            ActiveChunkGroupPayload::encode, ActiveChunkGroupPayload::decode);
    private static void encode(RegistryFriendlyByteBuf buffer, ActiveChunkGroupPayload payload) {
        buffer.writeUtf(payload.shape.id());
        buffer.writeVarInt(payload.shape.groupChunks());
        buffer.writeVarInt(payload.shape.cells().size());
        for (ChunkGroupShape.Cell cell : payload.shape.cells()) {
            buffer.writeVarInt(cell.x());
            buffer.writeVarInt(cell.z());
        }
        buffer.writeBoolean(payload.archipelago());
    }

    private static ActiveChunkGroupPayload decode(RegistryFriendlyByteBuf buffer) {
        String id = buffer.readUtf();
        int groupChunks = buffer.readVarInt();
        int size = buffer.readVarInt();
        List<ChunkGroupShape.Cell> cells = new ArrayList<>(size);
        for (int i = 0; i < size; i++) cells.add(new ChunkGroupShape.Cell(buffer.readVarInt(), buffer.readVarInt()));
        return new ActiveChunkGroupPayload(new ChunkGroupShape(id, groupChunks, cells), buffer.readBoolean());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
