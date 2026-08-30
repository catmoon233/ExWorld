package net.exmo.exworld.world.model;

/** Strategic view-window constants plus the validated per-world physical scale. */
public final class WorldDimensions {
    /** The DimensionType mixin makes the vanilla Overworld's physical storage range 0..319. */
    public static final int MIN_BUILD_HEIGHT = 0;
    public static final int BUILD_HEIGHT = 320;
    /** One client snapshot covers a bounded window; the persisted world itself may grow beyond it. */
    public static final int MAP_SIZE = 128;
    public static final int MAP_MIN = -MAP_SIZE / 2;
    public static final int MAP_MAX_EXCLUSIVE = MAP_MIN + MAP_SIZE;
    public static final int DEFAULT_GROUP_CHUNKS = 4;
    public static final int MIN_GROUP_CHUNKS = 1;
    public static final int MAX_GROUP_CHUNKS = 16;
    /** Vanilla's maximum border diameter; keeping groups inside its half extent leaves normal chunk edges valid. */
    public static final int MAX_WORLD_BORDER_DIAMETER = 59_999_968;

    private WorldDimensions() {}

    public static int validateGroupChunks(int chunks) {
        if (chunks < MIN_GROUP_CHUNKS || chunks > MAX_GROUP_CHUNKS) {
            throw new IllegalArgumentException("group chunks must be between " + MIN_GROUP_CHUNKS + " and " + MAX_GROUP_CHUNKS);
        }
        return chunks;
    }

    public static int groupBlocks(int groupChunks) { return validateGroupChunks(groupChunks) * 16; }
    public static int chunksPerGroup(int groupChunks) { return groupChunks * groupChunks; }

    /** Offset keeps every group edge on a native 16-block chunk edge for both odd and even sizes. */
    public static int alignmentOffsetBlocks(int groupChunks) {
        return validateGroupChunks(groupChunks) / 2 * 16;
    }

    public static int groupCoordinate(double worldCoordinate, int groupChunks) {
        return (int) Math.floor((worldCoordinate + alignmentOffsetBlocks(groupChunks)) / groupBlocks(groupChunks));
    }

    public static int groupCenter(int groupCoordinate, int groupChunks) {
        int blocks = groupBlocks(groupChunks);
        return groupCoordinate * blocks - alignmentOffsetBlocks(groupChunks) + blocks / 2;
    }

    public static int maximumGroupCoordinate(int groupChunks) {
        return (MAX_WORLD_BORDER_DIAMETER / 2 - groupBlocks(groupChunks)) / groupBlocks(groupChunks);
    }

    public static boolean withinMaximumWorldBorder(int groupX, int groupZ, int groupChunks) {
        int maximum = maximumGroupCoordinate(groupChunks);
        return Math.abs((long) groupX) <= maximum && Math.abs((long) groupZ) <= maximum;
    }

    /** Continuous strategic coordinate; integer values are the centers of world tiles. */
    public static double mapCoordinate(double worldCoordinate, int groupChunks) {
        return (worldCoordinate + alignmentOffsetBlocks(groupChunks)) / groupBlocks(groupChunks) - 0.5;
    }

    /** Stable initial-window bit index for optional pre-generation, or -1 for dynamically expanded cells. */
    public static int generationIndex(int chunkX, int chunkZ, int groupChunks) {
        validateGroupChunks(groupChunks);
        int mapX = groupCoordinate(chunkX * 16.0 + 8.0, groupChunks);
        int mapZ = groupCoordinate(chunkZ * 16.0 + 8.0, groupChunks);
        if (mapX < MAP_MIN || mapX >= MAP_MAX_EXCLUSIVE || mapZ < MAP_MIN || mapZ >= MAP_MAX_EXCLUSIVE) return -1;
        int minimumChunkX = mapX * groupChunks - groupChunks / 2;
        int minimumChunkZ = mapZ * groupChunks - groupChunks / 2;
        int localX = chunkX - minimumChunkX;
        int localZ = chunkZ - minimumChunkZ;
        if (localX < 0 || localZ < 0 || localX >= groupChunks || localZ >= groupChunks) return -1;
        int tileIndex = (mapZ - MAP_MIN) * MAP_SIZE + mapX - MAP_MIN;
        return tileIndex * chunksPerGroup(groupChunks) + localZ * groupChunks + localX;
    }

    /** Inverse of {@link #generationIndex(int, int, int)} for optional background pre-generation. */
    public static NativeChunk chunkAtGenerationIndex(int index, int groupChunks) {
        int total = MAP_SIZE * MAP_SIZE * chunksPerGroup(groupChunks);
        if (index < 0 || index >= total) throw new IllegalArgumentException("generation index outside world extent: " + index);
        int chunksPerTile = chunksPerGroup(groupChunks);
        int tileIndex = index / chunksPerTile;
        int localIndex = index % chunksPerTile;
        int mapX = MAP_MIN + tileIndex % MAP_SIZE;
        int mapZ = MAP_MIN + tileIndex / MAP_SIZE;
        return new NativeChunk(mapX * groupChunks - groupChunks / 2 + localIndex % groupChunks,
                mapZ * groupChunks - groupChunks / 2 + localIndex / groupChunks);
    }

    public record NativeChunk(int x, int z) {}
}
