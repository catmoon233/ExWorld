package net.exmo.exworld.battle.model;

public enum BattleState {
    CREATING, LOADING_ARENA, TRANSFERRING, INTRO, DEPLOYMENT, FACTION_PHASE, RESOLVING,
    VICTORY, DEFEAT, ESCAPED, ABORTED, REWARD, RETURNING, CLEANUP;

    public boolean outcome() { return this == VICTORY || this == DEFEAT || this == ESCAPED || this == ABORTED; }
    public boolean terminal() { return this == CLEANUP; }
}
