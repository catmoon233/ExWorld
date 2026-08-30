package net.exmo.exworld.dungeon.model;

import net.exmo.exworld.battle.api.BattleId;

import java.util.*;

public record DungeonSnapshot(UUID runId, String dungeonId, DungeonRunState state, long seed,
                              int slotX, int slotZ, int roomIndex, String roomId,
                              Set<UUID> members, Set<String> claimedTreasures,
                              boolean bossDefeated, BattleId battleId) {
    public DungeonSnapshot {
        members = Set.copyOf(members); claimedTreasures = Set.copyOf(claimedTreasures);
    }
}
