package net.exmo.exworld.ship.interact;

import java.util.Optional;
import java.util.function.IntPredicate;

/** Local-space DDA through hull voxels. Shared by client picking and server interaction checks. */
public final class ShipRaycast {
    public record Hit(int x, int y, int z, double distance) {}

    @FunctionalInterface
    public interface Occupied { boolean test(int x, int y, int z); }

    private ShipRaycast() {}

    public static Optional<Hit> trace(double ox, double oy, double oz, double dx, double dy, double dz,
                                      double maxDistance, Occupied occupied) {
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-8 || maxDistance <= 0) return Optional.empty();
        dx /= length;
        dy /= length;
        dz /= length;
        int x = (int) Math.floor(ox);
        int y = (int) Math.floor(oy);
        int z = (int) Math.floor(oz);
        int stepX = dx < 0 ? -1 : 1;
        int stepY = dy < 0 ? -1 : 1;
        int stepZ = dz < 0 ? -1 : 1;
        double tDeltaX = dx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dx);
        double tDeltaY = dy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dy);
        double tDeltaZ = dz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dz);
        double tMaxX = dx == 0 ? Double.POSITIVE_INFINITY : ((dx > 0 ? (x + 1) - ox : ox - x) * tDeltaX);
        double tMaxY = dy == 0 ? Double.POSITIVE_INFINITY : ((dy > 0 ? (y + 1) - oy : oy - y) * tDeltaY);
        double tMaxZ = dz == 0 ? Double.POSITIVE_INFINITY : ((dz > 0 ? (z + 1) - oz : oz - z) * tDeltaZ);
        double distance = 0;
        for (int step = 0; step < 512 && distance <= maxDistance; step++) {
            if (occupied.test(x, y, z)) return Optional.of(new Hit(x, y, z, distance));
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                distance = tMaxX;
                x += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                distance = tMaxY;
                y += stepY;
                tMaxY += tDeltaY;
            } else {
                distance = tMaxZ;
                z += stepZ;
                tMaxZ += tDeltaZ;
            }
        }
        return Optional.empty();
    }

    public static Optional<Hit> tracePacked(double ox, double oy, double oz, double dx, double dy, double dz,
                                            double maxDistance, IntPredicate occupiedPacked) {
        Occupied occupied = (x, y, z) -> {
            if (x < 0 || y < 0 || z < 0 || x > 127 || y > 127 || z > 127) return false;
            return occupiedPacked.test((x & 0x7F) | ((y & 0x7F) << 7) | ((z & 0x7F) << 14));
        };
        return trace(ox, oy, oz, dx, dy, dz, maxDistance, occupied);
    }
}
