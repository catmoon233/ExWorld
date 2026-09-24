package net.exmo.exworld.npc.entity;

/** Pure route and stuck-step helpers. They never teleport and never rewrite a route. */
public final class NpcMovement {
    public static final int STUCK_LIMIT = 8;

    private NpcMovement() {}

    public static int nextIndex(int index, int count, boolean loop, boolean arrived) {
        if (count <= 0) return 0;
        int cursor = Math.max(0, Math.min(index, count - 1));
        if (!arrived) return cursor;
        int next = cursor + 1;
        if (next < count) return next;
        return loop ? 0 : cursor;
    }

    public static int stuckAfter(int stuck, boolean failed) {
        return failed ? Math.max(0, stuck) + 1 : 0;
    }

    public static boolean shouldYield(int stuck) {
        return stuck >= STUCK_LIMIT;
    }

    public static boolean arrived(double x, double y, double z, double tx, double ty, double tz, double radius) {
        double dx = x - tx;
        double dy = y - ty;
        double dz = z - tz;
        double limit = Math.max(0.5, radius);
        return dx * dx + dy * dy + dz * dz <= limit * limit;
    }

    public static double[] sideStep(double x, double z, float yawDegrees, int attempt) {
        double yaw = Math.toRadians(yawDegrees + 90.0 * (attempt % 2 == 0 ? 1 : -1));
        double distance = 0.8 + (attempt % 3) * 0.25;
        return new double[] { x + Math.sin(yaw) * distance, z + Math.cos(yaw) * distance };
    }
}
