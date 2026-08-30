package net.exmo.exworld.progress;

import net.minecraft.resources.ResourceLocation;

/** A server-observed fact; clients never submit objective progress. */
public record QuestObjective(Type type, ResourceLocation target, String tag, int amount, String dimension,
                             double x, double y, double z, double radius) {
    public enum Type { LOCATION, WORLD_TILE, KILL, INVENTORY, BLOCK_INTERACT, ENTITY_INTERACT }
    public QuestObjective {
        if (target == null) throw new IllegalArgumentException("objective target is required");
        if (amount < 1) throw new IllegalArgumentException("objective amount must be positive");
        if (type == Type.LOCATION && radius <= 0) throw new IllegalArgumentException("location radius must be positive");
        tag = tag == null ? "" : tag;
        dimension = dimension == null ? "" : dimension;
    }
}
