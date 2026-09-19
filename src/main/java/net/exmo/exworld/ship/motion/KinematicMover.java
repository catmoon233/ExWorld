package net.exmo.exworld.ship.motion;

/** Axis-separated kinematic step. No bounce, friction, mass or force integration. */
public final class KinematicMover {
    public record Vec(double x, double y, double z) {
        public static final Vec ZERO = new Vec(0, 0, 0);

        public Vec add(Vec other) { return new Vec(x + other.x, y + other.y, z + other.z); }
        public Vec scale(double scalar) { return new Vec(x * scalar, y * scalar, z * scalar); }
        public double length() { return Math.sqrt(x * x + y * y + z * z); }

        public Vec clamp(double maxSpeed) {
            if (maxSpeed <= 0) return ZERO;
            double length = length();
            if (length <= maxSpeed || length < 1.0e-8) return this;
            double scale = maxSpeed / length;
            return new Vec(x * scale, y * scale, z * scale);
        }
    }

    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public Box moved(double dx, double dy, double dz) {
            return new Box(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
        }
    }

    @FunctionalInterface public interface Solid { boolean blocked(Box box); }

    private KinematicMover() {}

    public static Vec step(Vec position, Vec velocity, double maxSpeed, Box hullAtOrigin, Solid world) {
        Vec clamped = velocity.clamp(maxSpeed);
        double x = position.x, y = position.y, z = position.z;
        if (Math.abs(clamped.x) > 1.0e-8) {
            Box moved = hullAtOrigin.moved(x + clamped.x, y, z);
            if (!world.blocked(moved)) x += clamped.x;
        }
        if (Math.abs(clamped.y) > 1.0e-8) {
            Box moved = hullAtOrigin.moved(x, y + clamped.y, z);
            if (!world.blocked(moved)) y += clamped.y;
        }
        if (Math.abs(clamped.z) > 1.0e-8) {
            Box moved = hullAtOrigin.moved(x, y, z + clamped.z);
            if (!world.blocked(moved)) z += clamped.z;
        }
        return new Vec(x, y, z);
    }
}
