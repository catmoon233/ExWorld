package net.exmo.exworld.client.dungeon;

import net.exmo.exworld.dungeon.model.DungeonSnapshot;

/** Client cache for the server-owned dungeon run indicator. */
public final class DungeonClient {
    private static volatile DungeonSnapshot snapshot;
    private DungeonClient() {}
    public static void install(DungeonSnapshot value) { snapshot = value; }
    public static DungeonSnapshot snapshot() { return snapshot; }
    public static boolean active() { return snapshot != null && !snapshot.state().terminal(); }
}
