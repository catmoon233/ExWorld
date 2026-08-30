package net.exmo.exworld.client.battle;

/** Bounded, camera-relative viewport movement for a tactical battle. */
public record BattleCameraPan(double x, double z) {
    public static final BattleCameraPan CENTERED = new BattleCameraPan(0.0D, 0.0D);
    private static final double STEP_PER_TICK = .22D;
    private static final double MAX_DISTANCE = 6.0D;

    public static BattleCameraPan move(BattleCameraPan current, float yawDegrees, float forward, float strafe, int arenaSize) {
        if (current == null) current = CENTERED;
        double length = Math.hypot(forward, strafe);
        if (length <= 1.0E-4D) return current;
        double scale = STEP_PER_TICK / Math.max(1.0D, length);
        double yaw = Math.toRadians(yawDegrees);
        double x = current.x - Math.sin(yaw) * forward * scale - Math.cos(yaw) * strafe * scale;
        double z = current.z + Math.cos(yaw) * forward * scale - Math.sin(yaw) * strafe * scale;
        double limit = Math.min(MAX_DISTANCE, Math.max(2.0D, arenaSize * .33D));
        double distance = Math.hypot(x, z);
        return distance <= limit ? new BattleCameraPan(x, z) : new BattleCameraPan(x * limit / distance, z * limit / distance);
    }
}
