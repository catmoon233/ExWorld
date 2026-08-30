package net.exmo.exworld.progress;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Validated immutable datapack quest graph. */
public final class QuestDefinition {
    private final ResourceLocation id;
    private final QuestKind kind;
    private final String titleKey, descriptionKey, entry;
    private final Map<String, QuestNode> nodes;

    public QuestDefinition(ResourceLocation id, QuestKind kind, String titleKey, String descriptionKey, String entry, Collection<QuestNode> nodes) {
        this.id = Objects.requireNonNull(id); this.kind = Objects.requireNonNull(kind);
        this.titleKey = Objects.requireNonNullElse(titleKey, ""); this.descriptionKey = Objects.requireNonNullElse(descriptionKey, "");
        this.entry = Objects.requireNonNull(entry); LinkedHashMap<String, QuestNode> values = new LinkedHashMap<>();
        for (QuestNode node : nodes) if (values.putIfAbsent(node.id(), node) != null) throw new IllegalArgumentException("duplicate quest node " + node.id());
        if (!values.containsKey(entry)) throw new IllegalArgumentException("quest entry node does not exist: " + entry);
        for (QuestNode node : values.values()) for (String next : node.next()) if (!values.containsKey(next)) throw new IllegalArgumentException("unknown next node " + next);
        rejectCycles(values, entry, new HashSet<>(), new HashSet<>()); this.nodes = Map.copyOf(values);
    }
    private static void rejectCycles(Map<String, QuestNode> nodes, String id, Set<String> visiting, Set<String> done) {
        if (done.contains(id)) return; if (!visiting.add(id)) throw new IllegalArgumentException("quest graph contains a cycle at " + id);
        for (String next : nodes.get(id).next()) rejectCycles(nodes, next, visiting, done);
        visiting.remove(id); done.add(id);
    }
    public ResourceLocation id() { return id; } public QuestKind kind() { return kind; }
    public String titleKey() { return titleKey; } public String descriptionKey() { return descriptionKey; }
    public String entry() { return entry; } public QuestNode node(String id) { return nodes.get(id); }
    public Collection<QuestNode> nodes() { return nodes.values(); }
}
