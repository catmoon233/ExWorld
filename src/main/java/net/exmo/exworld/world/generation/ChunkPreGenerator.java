package net.exmo.exworld.world.generation;

import net.exmo.exworld.world.model.WorldDimensions;
import net.exmo.exworld.world.storage.WorldStateData;
import net.minecraft.server.level.ServerLevel;

/** A restart-safe, one-chunk-per-tick generator inspired by Chunky's bounded queue. */
public final class ChunkPreGenerator {
    public void tick(ServerLevel level, WorldStateData state) {
        int cursor = state.nextMissingChunkIndex();
        if (cursor < 0) return;
        WorldDimensions.NativeChunk chunk = WorldDimensions.chunkAtGenerationIndex(cursor, state.groupChunks());
        level.getChunk(chunk.x(), chunk.z());
        state.markChunkGenerated(chunk.x(), chunk.z());
    }
}
