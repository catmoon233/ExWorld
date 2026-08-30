package net.exmo.exworld.progress;

import java.util.List;

/** A node completes only after every objective has reached its required amount. */
public record QuestNode(String id, String titleKey, String descriptionKey, List<QuestObjective> objectives,
                        List<String> next, List<QuestReward> rewards) {
    public QuestNode {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("node id is required");
        titleKey = titleKey == null ? "" : titleKey;
        descriptionKey = descriptionKey == null ? "" : descriptionKey;
        objectives = List.copyOf(objectives == null ? List.of() : objectives);
        next = List.copyOf(next == null ? List.of() : next);
        rewards = List.copyOf(rewards == null ? List.of() : rewards);
    }
}
