package net.exmo.exworld.monster;

import java.util.List;

/** Fully populated profile frozen into a battle seed. */
public record ResolvedMonsterProfile(String displayName, float maxHealth, float health, float maxMana, float mana,
                                    float manaPerPhase, double initiative, int movementPoints, int actionPoints,
                                    int initialHandSize, int drawPerPhase, List<MonsterProfilePatch.Skill> skills) {
    public ResolvedMonsterProfile { skills = List.copyOf(skills); }
    public static ResolvedMonsterProfile defaultHostile() {
        return new ResolvedMonsterProfile("", 20, 20, 60, 60, 12, 0, 3, 3, 3, 2,
                List.of(new MonsterProfilePatch.Skill("iron:irons_spellbooks:magic_arrow", 1),
                        new MonsterProfilePatch.Skill("iron:irons_spellbooks:firebolt", 1),
                        new MonsterProfilePatch.Skill("iron:irons_spellbooks:heal", 1)));
    }
    public ResolvedMonsterProfile apply(MonsterProfilePatch patch) {
        if (patch == null) return this;
        return new ResolvedMonsterProfile(or(patch.displayName(), displayName), positive(patch.maxHealth(), maxHealth), bounded(patch.health(), health, 0, positive(patch.maxHealth(), maxHealth)),
                nonNegative(patch.maxMana(), maxMana), bounded(patch.mana(), mana, 0, nonNegative(patch.maxMana(), maxMana)), nonNegative(patch.manaPerPhase(), manaPerPhase),
                or(patch.initiative(), initiative), integer(patch.movementPoints(), movementPoints, 0, 32), integer(patch.actionPoints(), actionPoints, 0, 16),
                integer(patch.initialHandSize(), initialHandSize, 0, 7), integer(patch.drawPerPhase(), drawPerPhase, 0, 7), patch.skills() == null ? skills : patch.skills());
    }
    private static String or(String value, String fallback) { return value == null ? fallback : value; }
    private static double or(Double value, double fallback) { return value == null ? fallback : value; }
    private static float positive(Float value, float fallback) { return value == null ? fallback : Math.max(1, value); }
    private static float nonNegative(Float value, float fallback) { return value == null ? fallback : Math.max(0, value); }
    private static float bounded(Float value, float fallback, float min, float max) { return value == null ? Math.min(max, Math.max(min, fallback)) : Math.min(max, Math.max(min, value)); }
    private static int integer(Integer value, int fallback, int min, int max) { return value == null ? fallback : Math.max(min, Math.min(max, value)); }
}
