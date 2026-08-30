package net.exmo.exworld.battle.model;

/** Determines only which faction opens a battle; the fixed faction order is retained afterwards. */
public enum EngagementAdvantage {
    INITIATIVE,
    PLAYER_AMBUSH,
    ENEMY_AMBUSH;

    /** World encounters always grant the opening faction to the entity that initiated the hit. */
    public static EngagementAdvantage fromWorldAttacker(java.util.UUID attackerId, java.util.UUID playerId) {
        return java.util.Objects.equals(attackerId, playerId) ? PLAYER_AMBUSH : ENEMY_AMBUSH;
    }
}
