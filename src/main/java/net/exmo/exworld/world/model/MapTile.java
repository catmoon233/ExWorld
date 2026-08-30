package net.exmo.exworld.world.model;

/** Compact map-facing world-tile projection: only the data needed to draw and edit the strategic map. */
public record MapTile(String id, int mapX, int mapZ, String regionId, String biomeId) {
    public static MapTile from(WorldTile tile) {
        return new MapTile(tile.id(), tile.mapX(), tile.mapZ(), tile.regionId(), tile.biomeId());
    }

    public WorldBiome biome() { return WorldBiome.byId(biomeId); }
}
