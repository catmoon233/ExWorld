package net.exmo.exworld.npc.data;

/** A named standing point. Home, a stall and a post are all places referenced by id. */
public record NpcPlace(String id, double x, double y, double z, String dimension, float yaw, double radius) {
    public NpcPlace {
        id = id == null ? "" : id.trim();
        dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension.trim();
        radius = radius <= 0 ? 1.5 : radius;
    }
}
