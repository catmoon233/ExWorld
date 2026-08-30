package net.exmo.exworld.progress;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Atomic content snapshot shared by commands, events, and reward validation. */
public final class QuestContent {
    private static volatile Map<ResourceLocation, QuestDefinition> definitions = Map.of();
    private QuestContent() {}
    public static Optional<QuestDefinition> definition(ResourceLocation id) { return Optional.ofNullable(definitions.get(id)); }
    public static Collection<QuestDefinition> definitions() { return definitions.values(); }
    public static void replace(Collection<QuestDefinition> values) {
        LinkedHashMap<ResourceLocation, QuestDefinition> next = new LinkedHashMap<>();
        for (QuestDefinition value : values) if (next.putIfAbsent(value.id(), value) != null) throw new IllegalArgumentException("duplicate quest " + value.id());
        definitions = Map.copyOf(next);
    }
}
