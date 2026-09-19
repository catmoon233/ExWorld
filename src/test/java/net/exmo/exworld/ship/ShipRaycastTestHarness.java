package net.exmo.exworld.ship;

import net.exmo.exworld.ship.interact.ShipRaycast;
import net.exmo.exworld.ship.model.ShipHull;

/** Local DDA must hit the first occupied voxel along the ray. */
public final class ShipRaycastTestHarness {
    public static void main(String[] args) {
        ShipHull hull = ShipHull.builder()
                .add(0, 0, 0, "minecraft:oak_planks")
                .add(2, 0, 0, "minecraft:chest")
                .add(2, 1, 0, "minecraft:oak_planks")
                .build();
        var miss = ShipRaycast.trace(8, 0.5, 0.5, 1, 0, 0, 4, hull::occupied);
        require(miss.isEmpty(), "ray pointing away from the hull must miss");
        var hit = ShipRaycast.trace(-1, 0.5, 0.5, 1, 0, 0, 8, hull::occupied).orElseThrow();
        require(hit.x() == 0 && hit.y() == 0 && hit.z() == 0, "first plank must be hit");
        var chest = ShipRaycast.trace(1.5, 0.5, 0.5, 1, 0, 0, 8, hull::occupied).orElseThrow();
        require(chest.x() == 2 && chest.y() == 0, "chest voxel must be hit after the gap");
        var up = ShipRaycast.trace(2.5, -1, 0.5, 0, 1, 0, 8, hull::occupied).orElseThrow();
        require(up.x() == 2 && up.y() == 0, "upward ray should strike the chest first");
        System.out.println("SHIP_RAYCAST_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
