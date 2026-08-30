package net.exmo.exworld.battle.api;

import java.util.UUID;

/**
 * Describes where a battle is presented. The battle rules do not care whether
 * the host is the legacy isolated arena dimension or a room in a dungeon.
 */
public record BattleHost(String dimension, int originX, int originZ, int floorY,
                         boolean ownsArena, UUID dungeonRunId, String roomId) {
    public BattleHost {
        if (dimension == null || dimension.isBlank()) throw new IllegalArgumentException("Battle host dimension is required");
        roomId = roomId == null ? "" : roomId;
    }

    public static BattleHost isolated() {
        return new BattleHost("exworld:battle", 0, 0, 64, true, null, "");
    }

    public static BattleHost dungeon(String dimension, int originX, int originZ, int floorY,
                                     UUID runId, String roomId) {
        return new BattleHost(dimension, originX, originZ, floorY, false, runId, roomId);
    }

    public boolean dungeon() { return dungeonRunId != null; }
}
