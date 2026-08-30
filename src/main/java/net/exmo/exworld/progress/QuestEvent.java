package net.exmo.exworld.progress;

import net.minecraft.resources.ResourceLocation;

/** Normalized server observation; it carries no client-controlled progress. */
public record QuestEvent(QuestObjective.Type type, ResourceLocation target, String tag, String dimension,
                         double x, double y, double z, int amount) {
    public QuestEvent { tag = tag == null ? "" : tag; dimension = dimension == null ? "" : dimension; }
}
