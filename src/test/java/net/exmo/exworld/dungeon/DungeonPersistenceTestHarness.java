package net.exmo.exworld.dungeon;

import net.exmo.exworld.battle.api.*;
import net.exmo.exworld.battle.arena.*;
import net.exmo.exworld.battle.model.*;
import net.exmo.exworld.battle.skill.SkillRegistry;
import net.exmo.exworld.dungeon.model.DungeonRunState;
import net.exmo.exworld.dungeon.model.DungeonSnapshot;

import java.util.*;

public final class DungeonPersistenceTestHarness {
    public static void main(String[] args) {
        UUID player = UUID.randomUUID();
        DungeonRun run = new DungeonRun(UUID.randomUUID(), "test:dungeon", 17, 512, 1024, List.of(player));
        run.state(DungeonRunState.BATTLE); run.roomIndex(2); run.bossDefeated(true); run.claimTreasure("chest");
        run.battleId(new BattleId(UUID.randomUUID()));
        var snapshot = run.snapshot("combat");
        var restored = new DungeonSnapshot(snapshot.runId(), snapshot.dungeonId(), snapshot.state(), snapshot.seed(),
                snapshot.slotX(), snapshot.slotZ(), snapshot.roomIndex(), snapshot.roomId(), snapshot.members(),
                snapshot.claimedTreasures(), snapshot.bossDefeated(), snapshot.battleId());
        check(restored.runId().equals(run.id()) && restored.state() == DungeonRunState.BATTLE, "dungeon state survives snapshot persistence");
        check(restored.slotX() == 512 && restored.slotZ() == 1024 && restored.bossDefeated(), "slot and boss flag survive snapshot persistence");
        check(restored.claimedTreasures().contains("chest") && restored.battleId().equals(run.battleId()), "progress and battle survive snapshot persistence");

        UUID enemy = UUID.randomUUID();
        EncounterRequest request = new EncounterRequest("test:dungeon_battle", "test:arena",
                List.of(seed(player, player, "players", 1), seed(enemy, null, "enemies", 0)),
                Map.of(new EncounterRequest.FactionPair("players", "enemies"), FactionRelation.HOSTILE,
                        new EncounterRequest.FactionPair("enemies", "players"), FactionRelation.HOSTILE), Map.of(), 7,
                Map.of(), "players", null, null,
                BattleHost.dungeon("exworld:dungeon", 512, 1024, 64, run.id(), "combat"));
        var session = new net.exmo.exworld.battle.BattleSession(BattleId.create(), request,
                new ArenaGrid(ArenaDefinition.flat("test:arena", 8, 64)), SkillRegistry.defaults(), 512, 1024);
        check(request.host().dungeon() && request.host().dungeonRunId().equals(run.id()), "battle host survives request persistence");
        check(request.host().originX() == 512 && request.host().originZ() == 1024, "battle host coordinates survive request persistence");
    }
    private static EncounterRequest.CombatantSeed seed(UUID id, UUID player, String faction, int x) {
        return new EncounterRequest.CombatantSeed(id, player, id.toString(), faction, new BattleCell(x, 1, 64),
                40, 40, 60, 60, 10, 10, 4, 3, List.of("exworld:guarded_strike"));
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
