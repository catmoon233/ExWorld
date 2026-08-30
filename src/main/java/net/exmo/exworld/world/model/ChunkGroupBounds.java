package net.exmo.exworld.world.model;

/** The single spatial contract shared by movement-facing camera, visibility and boundary rendering. */
public record ChunkGroupBounds(int groupX, int groupZ, double minX, double minZ, double maxX, double maxZ) {
    public static ChunkGroupBounds containing(double worldX, double worldZ, int groupChunks) {
        int groupX = WorldDimensions.groupCoordinate(worldX, groupChunks);
        int groupZ = WorldDimensions.groupCoordinate(worldZ, groupChunks);
        return forGroup(groupX, groupZ, groupChunks);
    }

    public static ChunkGroupBounds forGroup(int groupX, int groupZ, int groupChunks) {
        int size = WorldDimensions.groupBlocks(groupChunks);
        double minX = groupX * (double) size - WorldDimensions.alignmentOffsetBlocks(groupChunks);
        double minZ = groupZ * (double) size - WorldDimensions.alignmentOffsetBlocks(groupChunks);
        return new ChunkGroupBounds(groupX, groupZ, minX, minZ, minX + size, minZ + size);
    }

    public boolean overlapsSection(int sectionX, int sectionZ) {
        return sectionX < maxX && sectionX + 16 > minX && sectionZ < maxZ && sectionZ + 16 > minZ;
    }
}
