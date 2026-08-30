package net.exmo.exworld.battle.model;

import net.exmo.exworld.battle.api.BattleEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Shared facing helpers for battle presentation. */
public final class BattleFacing {
    private BattleFacing() {}

    public static List<UUID> skillTargetIdsNewestFirst(List<BattleEvent> events, UUID actorId) {
        List<UUID> targets = new ArrayList<>();
        for (int index = events.size() - 1; index >= 0; index--) {
            BattleEvent event = events.get(index);
            if (event.type() == BattleEvent.Type.SKILL && actorId.equals(event.actorId()) && event.targetId() != null)
                targets.add(event.targetId());
        }
        return List.copyOf(targets);
    }

    public static float approachYaw(float currentYaw, BattleCell from, BattleCell to, float maximumChange) {
        if (sameCoordinates(from, to)) return currentYaw;
        float targetYaw = BattleMovementHeading.yawDegrees(from, to);
        float difference = wrapDegrees(targetYaw - currentYaw);
        return currentYaw + Math.max(-maximumChange, Math.min(maximumChange, difference));
    }

    public static float approachPitch(float currentPitch, float targetPitch, float maximumChange) {
        return currentPitch + Math.max(-maximumChange, Math.min(maximumChange, targetPitch - currentPitch));
    }

    private static float wrapDegrees(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }

    private static boolean sameCoordinates(BattleCell left, BattleCell right) {
        return left.x() == right.x() && left.z() == right.z();
    }
}
