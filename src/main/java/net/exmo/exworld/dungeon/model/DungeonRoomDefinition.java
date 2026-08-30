package net.exmo.exworld.dungeon.model;

import net.exmo.exworld.battle.arena.ArenaDefinition;

import java.util.*;

public record DungeonRoomDefinition(String id, DungeonRoomType type, int width, int depth,
                                    int floorY, String arenaId, int alertRadius,
                                    Set<ArenaDefinition.GridPoint> blocked,
                                    List<DungeonEnemyDefinition> enemies, int goldReward,
                                    ArenaDefinition.GridPoint entrance, ArenaDefinition.GridPoint exit,
                                    List<ArenaDefinition.GridPoint> enemySpawns) {
    public DungeonRoomDefinition(String id, DungeonRoomType type, int width, int depth, int floorY, String arenaId,
                                 int alertRadius, Set<ArenaDefinition.GridPoint> blocked,
                                 List<DungeonEnemyDefinition> enemies, int goldReward) {
        this(id,type,width,depth,floorY,arenaId,alertRadius,blocked,enemies,goldReward,null,null,List.of());
    }
    public DungeonRoomDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Room id is required");
        if (width < 8 || depth < 8) throw new IllegalArgumentException("Dungeon room is too small");
        type = Objects.requireNonNull(type); floorY = floorY == 0 ? 64 : floorY;
        arenaId = arenaId == null || arenaId.isBlank() ? "exworld:flat_18" : arenaId;
        alertRadius = Math.max(1, alertRadius); blocked = Set.copyOf(blocked); enemies = List.copyOf(enemies);
        goldReward = Math.max(0, goldReward); entrance=entrance==null?new ArenaDefinition.GridPoint(width/2,0):entrance;
        exit=exit==null?new ArenaDefinition.GridPoint(width/2,depth-1):exit; enemySpawns=List.copyOf(enemySpawns);
    }
}
