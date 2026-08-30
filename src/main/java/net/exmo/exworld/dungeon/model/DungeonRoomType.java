package net.exmo.exworld.dungeon.model;

public enum DungeonRoomType {
    ENTRANCE, COMBAT, ELITE, TREASURE, BOSS, EXIT;

    public boolean battle() { return this == COMBAT || this == ELITE || this == BOSS; }
}
