package net.exmo.exworld.battle;

import net.exmo.exworld.battle.action.BattleActionTimeline;
import net.exmo.exworld.battle.api.BattleEvent;
import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.BattleFacing;
import net.exmo.exworld.client.battle.BattleMovementInterpolator;
import net.exmo.exworld.battle.model.BattleMovementHeading;

import java.util.List;
import java.util.UUID;

public final class BattleMovementPresentationTestHarness {
    public static void main(String[] args) {
        UUID actor = UUID.randomUUID();
        BattleCell start = new BattleCell(0, 0, 64);
        List<BattleCell> path = List.of(new BattleCell(1, 0, 64), new BattleCell(2, 0, 64), new BattleCell(3, 0, 64));
        BattleActionTimeline timeline = new BattleActionTimeline();
        check(timeline.startMove(actor, start, path), "movement starts");
        check(!timeline.startMove(actor, start, path), "busy actor cannot start another move");
        double previous = -1;
        int completions = 0;
        for (int tick = 0; tick < path.size() * BattleActionTimeline.TICKS_PER_CELL; tick++) {
            BattleActionTimeline.MoveAction action = timeline.moves().iterator().next();
            double progress = action.progress(0);
            check(progress >= previous, "presentation path is monotonic"); previous = progress;
            completions += timeline.tick().size();
        }
        check(completions == 1, "movement produces one final commit signal");
        check(timeline.moves().isEmpty(), "completed movement is removed");
        double prior=-1;for(int i=0;i<=100;i++){double input=i/100.0;double value=BattleMovementInterpolator.segmentProgress(input);check(value>=prior,"movement interpolation stays monotonic");check(Math.abs(value-input)<.000001,"movement keeps constant speed across cell boundaries");prior=value;}
        var clock=new net.exmo.exworld.client.battle.BattleMotionClock();clock.sync(2,100);check(clock.sample(100,0,10)==2,"first snapshot initializes without using absolute world time");double sampled=clock.sample(101,.5F,10);clock.sync(2,101);
        check(clock.sample(101,.5F,10)>=sampled,"stale movement snapshots never rewind client presentation");
        check(Math.abs(BattleMovementHeading.yawDegrees(new BattleCell(0,0,64),new BattleCell(1,0,64))+90)<.001,
                "eastbound movement faces east");
        check(Math.abs(BattleMovementHeading.yawDegrees(new BattleCell(0,0,64),new BattleCell(0,1,64)))<.001,
                "southbound movement faces south");
        UUID earlierTarget = UUID.randomUUID(), latestTarget = UUID.randomUUID();
        List<BattleEvent> attacks = List.of(
                new BattleEvent(8, 1, BattleEvent.Type.SKILL, actor, earlierTarget, "actor", "target", "test:spell", "skill.test", 0),
                new BattleEvent(9, 1, BattleEvent.Type.SKILL, actor, latestTarget, "actor", "target", "test:spell", "skill.test", 0));
        check(BattleFacing.skillTargetIdsNewestFirst(attacks, actor).equals(List.of(latestTarget, earlierTarget)),
                "post-move facing prioritizes the most recently attacked target");
        check(Math.abs(BattleFacing.approachYaw(0, start, new BattleCell(1, 0, 64), 12) + 12) < .001,
                "post-move facing turns in capped increments instead of snapping to the target");
        check(Math.abs(BattleFacing.approachPitch(36, 0, 12) - 24) < .001,
                "self-cast recovery raises the view gradually instead of leaving it pointed down");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
