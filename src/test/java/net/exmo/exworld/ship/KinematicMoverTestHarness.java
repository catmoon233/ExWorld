package net.exmo.exworld.ship;

import net.exmo.exworld.ship.motion.KinematicMover;
import net.exmo.exworld.ship.motion.KinematicMover.Box;
import net.exmo.exworld.ship.motion.KinematicMover.Vec;

/** Speed clamp and per-axis rejection without bounce. */
public final class KinematicMoverTestHarness {
    public static void main(String[] args) {
        Vec clamped = new Vec(6, 0, 0).clamp(0.5);
        require(Math.abs(clamped.x() - 0.5) < 1.0e-9 && clamped.y() == 0, "velocity must clamp to max speed");
        Box hull = new Box(0, 0, 0, 2, 1, 1);
        Vec free = KinematicMover.step(new Vec(0, 10, 0), new Vec(0.4, 0, 0), 1, hull, box -> false);
        require(Math.abs(free.x() - 0.4) < 1.0e-9, "unblocked axis must advance");
        Vec blocked = KinematicMover.step(new Vec(0, 10, 0), new Vec(0.4, 0.2, 0), 1, hull, box -> box.minX() > 0.2);
        require(Math.abs(blocked.x()) < 1.0e-9, "blocked X must stop");
        require(Math.abs(blocked.y() - 10.2) < 1.0e-9, "free Y must still advance");
        Vec overspeed = KinematicMover.step(Vec.ZERO, new Vec(10, 0, 0), 0.25, hull, box -> false);
        require(Math.abs(overspeed.x() - 0.25) < 1.0e-9, "step must use the clamped speed");
        System.out.println("KINEMATIC_MOVER_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
