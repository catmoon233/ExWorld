package net.exmo.exworld.world.model;

/** A strategic-map cell. It is deliberately distinct from a Minecraft chunk and has no persistence concerns. */
public record WorldTile(
        String id, int mapX, int mapZ, String regionId, String name, int color,
        int worldX, int worldZ, boolean discovered,
        String biomeId, String description, String sites, String resources
) {
    public WorldBiome biome() { return WorldBiome.byId(biomeId); }
}
