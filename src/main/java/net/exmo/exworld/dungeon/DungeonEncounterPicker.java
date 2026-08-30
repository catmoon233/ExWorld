package net.exmo.exworld.dungeon;

import net.exmo.exworld.dungeon.model.DungeonEnemyDefinition;

import java.util.List;

/** Seeded content selection kept deterministic and independent from live entities. */
public final class DungeonEncounterPicker {
    private DungeonEncounterPicker() {}
    public static DungeonEnemyDefinition pick(List<DungeonEnemyDefinition> pool, long seed) {
        if (pool.isEmpty()) throw new IllegalArgumentException("Enemy pool is empty");
        return pool.get(new java.util.SplittableRandom(seed).nextInt(pool.size()));
    }
}
