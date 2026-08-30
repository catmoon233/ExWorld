package net.exmo.exworld.monster;

import java.util.Objects;

/** A named, reusable monster profile fragment supplied by an ExWorld monster pack. */
public record MonsterTemplate(String id, MonsterProfilePatch profile) {
    public MonsterTemplate {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("template id is required");
        profile = Objects.requireNonNullElse(profile, MonsterProfilePatch.empty());
    }
}
