package net.exmo.exworld.ship;

import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipOccupancy;
import net.exmo.exworld.ship.model.ShipPart;
import net.exmo.exworld.ship.model.ShipTemplate;
import net.exmo.exworld.ship.storage.ShipNbtCodec;
import net.exmo.exworld.ship.storage.ShipTemplates;

import java.util.HashSet;
import java.util.Set;

/** Verifies the built-in temple template is well-formed and round-trips through the codec. */
public final class ShipTemplatesTestHarness {
    public static void main(String[] args) {
        ShipTemplate temple = ShipTemplates.temple();
        check(ShipTemplates.TEMPLE_ID.equals(temple.id()) ? null : "unexpected id " + temple.id());
        check(temple.name() != null && !temple.name().isBlank() ? null : "blank name");
        check(!temple.hull().isEmpty() ? null : "empty hull");
        check(temple.hull().size() > 40 ? null : "temple hull too small: " + temple.hull().size());
        check(temple.parts().size() == 4 ? null : "expected 4 parts, got " + temple.parts().size());

        Set<Integer> all = new HashSet<>();
        int sum = 0;
        for (ShipPart part : temple.parts()) {
            int[] occupancy = part.occupancy();
            check(occupancy.length > 0 ? null : "part " + part.id() + " has empty occupancy");
            for (int packed : occupancy) {
                check(!all.contains(packed) ? null : "part " + part.id() + " overlaps another part at " + packed);
                all.add(packed);
                int x = ShipOccupancy.x(packed), y = ShipOccupancy.y(packed), z = ShipOccupancy.z(packed);
                check(temple.hull().occupied(x, y, z) ? null : "part " + part.id() + " voxel outside hull: " + x + "," + y + "," + z);
            }
            sum += occupancy.length;
        }
        check(all.size() == sum ? null : "part masks overlap");

        boolean helm = false, core = false;
        for (var block : temple.hull().blocks()) {
            if (block.block().contains("ship_helm")) helm = true;
            if (block.block().contains("ship_core")) core = true;
        }
        check(helm ? null : "missing helm block");
        check(core ? null : "missing core block");

        ShipTemplate decoded = ShipNbtCodec.decodeTemplate(ShipNbtCodec.encodeTemplate(temple));
        check(temple.equals(decoded) ? null : "codec round-trip mismatch");
        check(decoded.hull().size() == temple.hull().size() ? null : "round-trip hull size differs");

        System.out.println("SHIP_TEMPLE_TEST_OK");
    }

    private static void check(String error) {
        if (error != null) throw new IllegalStateException(error);
    }
}
