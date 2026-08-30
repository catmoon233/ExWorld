package net.exmo.exworld.world.model;

import java.util.List;

public record WorldSnapshot(List<MapTile> tiles, String currentTileId, int generatedChunks, int totalChunks,
                            int mapMinimumX, int mapMinimumZ, int mapWidth, int mapHeight, int groupChunks,
                            boolean pregenerationEnabled, boolean manualGroups, List<MapRegion> regions, List<MapAnchor> anchors,
                            long groupRevision) {
    public WorldSnapshot {
        tiles = List.copyOf(tiles);
        regions = List.copyOf(regions);
        anchors = List.copyOf(anchors);
    }
    public WorldSnapshot(List<MapTile> tiles, String currentTileId, int generatedChunks, int totalChunks,
                         int mapMinimum, int mapSize, int groupChunks, boolean pregenerationEnabled, boolean manualGroups,
                         List<MapRegion> regions, List<MapAnchor> anchors) {
        this(tiles, currentTileId, generatedChunks, totalChunks, mapMinimum, mapMinimum, mapSize, mapSize, groupChunks,
                pregenerationEnabled, manualGroups, regions, anchors, 0L);
    }
}
