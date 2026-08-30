package net.exmo.exworld.dungeon;

import net.exmo.exworld.dungeon.model.*;
import java.util.*;

/** Pure state transition module used by runtime orchestration and tests. */
public final class DungeonEngine {
    public DungeonSnapshot enter(DungeonDefinition definition, UUID runId, long seed, int slotX, int slotZ, Collection<UUID> members) {
        if (members.isEmpty()) throw new IllegalArgumentException("Dungeon requires at least one member");
        DungeonRun run = new DungeonRun(runId, definition.id(), seed, slotX, slotZ, List.copyOf(members));
        run.state(DungeonRunState.EXPLORING);
        return run.snapshot(definition.rooms().getFirst().id());
    }
    public DungeonSnapshot battleStarted(DungeonRun run, UUID battleId, String roomId) {
        if (run.state() != DungeonRunState.EXPLORING) throw new IllegalStateException("Dungeon is not exploring");
        run.battleId(new net.exmo.exworld.battle.api.BattleId(battleId)); run.state(DungeonRunState.BATTLE); return run.snapshot(roomId);
    }
    public DungeonSnapshot battleFinished(DungeonRun run, DungeonDefinition definition, boolean victory) {
        if (run.state() != DungeonRunState.BATTLE) throw new IllegalStateException("Dungeon is not in battle");
        DungeonRoomDefinition room=currentRoom(definition,run); if(!victory){run.state(DungeonRunState.FAILED);return run.snapshot(room.id());}
        run.completeRoom(room.id());if(room.type()==DungeonRoomType.BOSS)run.bossDefeated(true);run.battleId(null);run.roomIndex(run.roomIndex()+1);
        if(run.roomIndex()>=definition.rooms().size())run.state(DungeonRunState.COMPLETED);else run.state(DungeonRunState.EXPLORING);
        return run.snapshot(run.roomIndex()<definition.rooms().size()?definition.rooms().get(run.roomIndex()).id():room.id());
    }
    public DungeonSnapshot claimTreasure(DungeonRun run,DungeonDefinition definition){DungeonRoomDefinition room=currentRoom(definition,run);if(room.type()!=DungeonRoomType.TREASURE||!run.claimTreasure(room.id()))return run.snapshot(room.id());run.completeRoom(room.id());run.roomIndex(run.roomIndex()+1);if(run.roomIndex()>=definition.rooms().size())run.state(DungeonRunState.COMPLETED);return run.snapshot(run.roomIndex()<definition.rooms().size()?definition.rooms().get(run.roomIndex()).id():room.id());}
    public DungeonSnapshot fail(DungeonRun run,String roomId){run.state(DungeonRunState.FAILED);return run.snapshot(roomId);}
    private static DungeonRoomDefinition currentRoom(DungeonDefinition definition,DungeonRun run){return definition.rooms().get(Math.min(run.roomIndex(),definition.rooms().size()-1));}
}
