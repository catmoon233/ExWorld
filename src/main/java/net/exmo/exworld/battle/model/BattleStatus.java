package net.exmo.exworld.battle.model;

/** Battle-local status instance. Durations count down when the owning faction begins a phase. */
public record BattleStatus(String id, String nameKey, int stacks, int remainingRounds, boolean beneficial, boolean permanent,
                           boolean potion, boolean preservesPotionLevel) {
    public BattleStatus(String id, String nameKey, int stacks, int remainingRounds, boolean beneficial) {
        this(id, nameKey, stacks, remainingRounds, beneficial, false, false, false);
    }
    public BattleStatus(String id, String nameKey, int stacks, int remainingRounds, boolean beneficial, boolean permanent) {
        this(id, nameKey, stacks, remainingRounds, beneficial, permanent, false, false);
    }
    public BattleStatus {
        stacks = Math.max(1, stacks);
        remainingRounds = permanent ? Integer.MAX_VALUE : Math.max(1, remainingRounds);
    }

    public BattleStatus merge(int addedStacks, int duration) {
        return new BattleStatus(id, nameKey, Math.min(99, stacks + Math.max(1, addedStacks)),
                permanent ? Integer.MAX_VALUE : Math.max(remainingRounds, duration), beneficial, permanent, potion, preservesPotionLevel);
    }

    public BattleStatus tick() {
        return permanent ? this : new BattleStatus(id, nameKey, stacks, Math.max(1, remainingRounds - 1), beneficial,
                false, potion, preservesPotionLevel);
    }

    /** Potion effects lose one displayed level per owner phase unless their original duration is long-lived. */
    public BattleStatus lowerPotionLevel() {
        return new BattleStatus(id, nameKey, stacks - 1, remainingRounds, beneficial, permanent, true, preservesPotionLevel);
    }
}
