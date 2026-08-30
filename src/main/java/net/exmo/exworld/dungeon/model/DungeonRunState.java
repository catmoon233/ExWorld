package net.exmo.exworld.dungeon.model;

public enum DungeonRunState {
    ENTERING, EXPLORING, BATTLE, COMPLETED, FAILED, CLEANUP;

    public boolean terminal() { return this == COMPLETED || this == CLEANUP; }
    public boolean failed() { return this == FAILED; }
}
