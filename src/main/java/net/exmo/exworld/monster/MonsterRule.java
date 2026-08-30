package net.exmo.exworld.monster;

import java.util.Objects;

/** A deterministic selector rule. A rule can reference a template and then override individual fields. */
public record MonsterRule(String id, MonsterSelector selector, String templateId, MonsterProfilePatch profile, int priority) {
    public MonsterRule {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("rule id is required");
        selector = Objects.requireNonNull(selector, "selector");
        templateId = templateId == null ? "" : templateId;
        profile = Objects.requireNonNullElse(profile, MonsterProfilePatch.empty());
    }
}
