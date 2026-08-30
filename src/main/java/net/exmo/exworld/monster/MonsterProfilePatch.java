package net.exmo.exworld.monster;

import java.util.List;

/** Nullable fields deliberately mean "inherit from the lower-priority profile". */
public record MonsterProfilePatch(String displayName, Float maxHealth, Float health, Float maxMana, Float mana,
                                  Float manaPerPhase, Double initiative, Integer movementPoints, Integer actionPoints,
                                  Integer initialHandSize, Integer drawPerPhase, List<Skill> skills) {
    public record Skill(String id, int star) {
        public Skill { if (id == null || id.isBlank()) throw new IllegalArgumentException("skill id is required"); star = Math.max(1, Math.min(5, star)); }
    }
    public MonsterProfilePatch { skills = skills == null ? null : List.copyOf(skills); }
    public static MonsterProfilePatch empty() { return new MonsterProfilePatch(null, null, null, null, null, null, null, null, null, null, null, null); }
    public MonsterProfilePatch overlay(MonsterProfilePatch upper) {
        if (upper == null) return this;
        return new MonsterProfilePatch(value(upper.displayName, displayName), value(upper.maxHealth, maxHealth), value(upper.health, health),
                value(upper.maxMana, maxMana), value(upper.mana, mana), value(upper.manaPerPhase, manaPerPhase),
                value(upper.initiative, initiative), value(upper.movementPoints, movementPoints), value(upper.actionPoints, actionPoints),
                value(upper.initialHandSize, initialHandSize), value(upper.drawPerPhase, drawPerPhase), upper.skills == null ? skills : upper.skills);
    }
    private static <T> T value(T upper, T lower) { return upper == null ? lower : upper; }
}
