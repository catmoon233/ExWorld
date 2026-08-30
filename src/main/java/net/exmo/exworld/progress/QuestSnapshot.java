package net.exmo.exworld.progress;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Network/UI projection, intentionally independent of all NBT implementation details. */
public record QuestSnapshot(ResourceLocation id, QuestKind kind, QuestStatus status, String titleKey, String descriptionKey,
                            String nodeId, String nodeTitleKey, String nodeDescriptionKey, List<Objective> objectives,
                            List<String> branches, ResourceLocation navigationTarget) {
    public QuestSnapshot { objectives = List.copyOf(objectives); branches = List.copyOf(branches); }
    public record Objective(QuestObjective.Type type, String target, int current, int required, String dimension, double x, double y, double z) {}
}
