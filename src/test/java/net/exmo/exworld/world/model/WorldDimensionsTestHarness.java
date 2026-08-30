package net.exmo.exworld.world.model;

/** Every configurable group size must align exactly to native chunk edges, including odd sizes. */
public final class WorldDimensionsTestHarness {
    public static void main(String[] args) {
        require(WorldDimensions.MIN_BUILD_HEIGHT == 0 && WorldDimensions.BUILD_HEIGHT == 320,
                "ExWorld height contract must start at Y=0 and end at Y=319");
        for (int chunks = WorldDimensions.MIN_GROUP_CHUNKS; chunks <= WorldDimensions.MAX_GROUP_CHUNKS; chunks++) {
            int blocks = WorldDimensions.groupBlocks(chunks);
            for (int group = -3; group <= 3; group++) {
                int center = WorldDimensions.groupCenter(group, chunks);
                ChunkGroupBounds bounds = ChunkGroupBounds.forGroup(group, 0, chunks);
                require(bounds.minX() % 16 == 0 && bounds.maxX() % 16 == 0, "edge is not chunk aligned");
                require(bounds.maxX() - bounds.minX() == blocks, "group width changed");
                require(WorldDimensions.groupCoordinate(center, chunks) == group, "center maps to wrong group");
                require(WorldDimensions.groupCoordinate(bounds.minX(), chunks) == group, "minimum edge maps wrong");
                require(WorldDimensions.groupCoordinate(bounds.maxX() - 0.001, chunks) == group, "maximum edge maps wrong");
                require(Math.abs(WorldDimensions.mapCoordinate(center, chunks) - group) < 1.0E-9,
                        "continuous map coordinate misses group center");
            }
            int total = WorldDimensions.MAP_SIZE * WorldDimensions.MAP_SIZE * WorldDimensions.chunksPerGroup(chunks);
            int[] probes = {0, 1, total / 3, total / 2, total - 2, total - 1};
            for (int index : probes) {
                WorldDimensions.NativeChunk chunk = WorldDimensions.chunkAtGenerationIndex(index, chunks);
                require(WorldDimensions.generationIndex(chunk.x(), chunk.z(), chunks) == index,
                        "generation index is not reversible at " + index + " for scale " + chunks);
            }
        }
        System.out.println("WORLD_DIMENSIONS_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
