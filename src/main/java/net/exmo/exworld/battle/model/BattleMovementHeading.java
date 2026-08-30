package net.exmo.exworld.battle.model;

/** Shared Minecraft yaw calculation for a single tactical movement segment. */
public final class BattleMovementHeading {
    private BattleMovementHeading() {}

    public static float yawDegrees(BattleCell from, BattleCell to) {
        double dx = to.x() - from.x(), dz = to.z() - from.z();
        if (dx == 0 && dz == 0) return 0;
        return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
    }
}
