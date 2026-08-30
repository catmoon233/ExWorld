package net.exmo.exworld.dungeon;

import net.exmo.exworld.battle.api.BattleId;
import net.exmo.exworld.battle.arena.ArenaDefinition;
import net.exmo.exworld.dungeon.model.*;

import java.util.*;

public final class DungeonRulesTestHarness {
    public static void main(String[] args) {
        UUID player = UUID.randomUUID();
        DungeonDefinition definition = definition();
        DungeonEngine engine = new DungeonEngine();
        DungeonSnapshot entered = engine.enter(definition, UUID.randomUUID(), 42, 0, 512, List.of(player));
        check(entered.state() == DungeonRunState.EXPLORING && entered.roomIndex() == 0, "enter starts exploration");

        DungeonRun run = new DungeonRun(entered.runId(), entered.dungeonId(), entered.seed(), entered.slotX(), entered.slotZ(), List.of(player));
        run.state(DungeonRunState.EXPLORING);
        run.roomIndex(1);
        engine.battleStarted(run, UUID.randomUUID(), "combat");
        DungeonSnapshot afterBattle = engine.battleFinished(run, definition, true);
        check(afterBattle.state() == DungeonRunState.EXPLORING && afterBattle.roomIndex() == 2, "battle advances to treasure room");
        DungeonSnapshot treasure = engine.claimTreasure(run, definition);
        check(treasure.roomIndex() == 3 && run.claimedTreasures().contains("treasure"), "treasure is claimed once and advances");
        DungeonSnapshot duplicate = engine.claimTreasure(run, definition);
        check(duplicate.roomIndex() == 3, "treasure cannot be claimed twice");

        engine.battleStarted(run, UUID.randomUUID(), "boss");
        DungeonSnapshot failed = engine.battleFinished(run, definition, false);
        check(failed.state() == DungeonRunState.FAILED, "failed battle ends the run");

        DungeonSlotAllocator.Slot first = DungeonSlotAllocator.allocate(List.of(run));
        check(first.x() != run.slotX() || first.z() != run.slotZ(), "allocator skips occupied slot");

        DungeonEnemyDefinition one = new DungeonEnemyDefinition("one", "minecraft:zombie", "one", 10, 10, 1, 1, 1, List.of());
        DungeonEnemyDefinition two = new DungeonEnemyDefinition("two", "minecraft:skeleton", "two", 10, 10, 1, 1, 1, List.of());
        check(DungeonEncounterPicker.pick(List.of(one, two), 99).id().equals(DungeonEncounterPicker.pick(List.of(one, two), 99).id()), "seeded enemy selection is stable");
    }

    private static DungeonDefinition definition() {
        DungeonRoomDefinition entrance = room("entrance", DungeonRoomType.ENTRANCE);
        DungeonRoomDefinition combat = room("combat", DungeonRoomType.COMBAT);
        DungeonRoomDefinition treasure = room("treasure", DungeonRoomType.TREASURE);
        DungeonRoomDefinition boss = room("boss", DungeonRoomType.BOSS);
        DungeonRoomDefinition exit = room("exit", DungeonRoomType.EXIT);
        return new DungeonDefinition("test:dungeon", "test.dungeon", 512, List.of(entrance, combat, treasure, boss, exit));
    }
    private static DungeonRoomDefinition room(String id, DungeonRoomType type) {
        return new DungeonRoomDefinition(id, type, 16, 16, 64, "test:arena", 8, Set.of(), List.of(), 25);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
