package net.exmo.exworld.world.model;

public record MapTile(String id, int mapX, int mapZ, String regionId, String biomeId, String sites) {
    public static MapTile from(WorldTile tile) {
        return new MapTile(tile.id(), tile.mapX(), tile.mapZ(), tile.regionId(), tile.biomeId(), tile.sites());
    }

    public MapTile(String id, int mapX, int mapZ, String regionId, String biomeId) {
        this(id, mapX, mapZ, regionId, biomeId, "");
    }

    public WorldBiome biome() { return WorldBiome.byId(biomeId); }
    public boolean island() { return !sites.isBlank() && !sites.equals("暂无已知据点"); }
}
