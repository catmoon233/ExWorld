package net.exmo.exworld.ship.motion;

import net.exmo.exworld.ship.model.ShipHull;

/**
 * Deck standing test. A player whose feet sit on a hull voxel is aboard; Y snaps to the voxel top.
 * This is not rigid-body collision.
 */
public final class AboardAttachment {
    public record Result(boolean aboard, int floorX, int floorY, int floorZ, double snapLocalY) {}

    private AboardAttachment() {}

    public static Result inspect(double localX, double localY, double localZ, ShipHull hull) {
        int x = (int) Math.floor(localX);
        int z = (int) Math.floor(localZ);
        int feet = (int) Math.floor(localY - 1.0e-4);
        if (hull.occupied(x, feet, z)) {
            return new Result(true, x, feet, z, feet + 1.0);
        }
        if (hull.occupied(x, feet - 1, z) && localY - feet < 0.6) {
            return new Result(true, x, feet - 1, z, feet);
        }
        return new Result(false, x, feet, z, localY);
    }
}
