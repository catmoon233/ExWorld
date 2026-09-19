package net.exmo.exworld.ship.upgrade;

import net.exmo.exworld.ship.model.PartSelection;
import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipOccupancy;
import net.exmo.exworld.ship.model.ShipPart;
import net.exmo.exworld.ship.model.ShipTemplate;

/** Validates template-constrained part swaps and rewrites only that occupancy. */
public final class ShipUpgradeRules {
    private ShipUpgradeRules() {}

    public static Result apply(ShipTemplate template, String partId, String variantId, ShipHull variantHull) {
        ShipPart part = template.part(partId);
        if (part.empty()) return Result.fail("part occupancy is empty");
        if (variantId == null || variantId.isBlank()) {
            return Result.fail("variant id is blank");
        }
        if (!part.allows(variantId)) return Result.fail("variant is not on the allowed list");
        if (variantHull == null || variantHull.isEmpty()) return Result.fail("variant hull is empty");
        if (!ShipOccupancy.sameShape(part.occupancy(), variantHull.occupancy())) {
            return Result.fail("variant occupancy does not match the part mask");
        }
        ShipHull next = template.hull().replaceOccupancy(part.occupancy(), variantHull);
        ShipTemplate updated = template.withHull(next).withSelection(template.selection().with(partId, variantId));
        return Result.ok(updated);
    }

    public static Result restoreBase(ShipTemplate template, String partId, ShipHull baseHull) {
        ShipPart part = template.part(partId);
        if (part.empty()) return Result.fail("part occupancy is empty");
        if (baseHull == null || baseHull.isEmpty()) return Result.fail("base hull is empty");
        if (!ShipOccupancy.sameShape(part.occupancy(), baseHull.occupancy())
                && !occupancyContained(part.occupancy(), baseHull)) {
            return Result.fail("base hull does not contain the part mask");
        }
        ShipHull slice = slice(baseHull, part.occupancy());
        ShipHull next = template.hull().replaceOccupancy(part.occupancy(), slice);
        return Result.ok(template.withHull(next).withSelection(template.selection().with(partId, "")));
    }

    public static boolean occupancyContained(int[] occupancy, ShipHull hull) {
        for (int packed : occupancy) if (!hull.occupiedPacked(packed)) return false;
        return true;
    }

    public static ShipHull slice(ShipHull hull, int[] occupancy) {
        var builder = ShipHull.builder();
        for (int packed : occupancy) {
            hull.blockAtPacked(packed).ifPresent(block ->
                    builder.add(ShipOccupancy.x(packed), ShipOccupancy.y(packed), ShipOccupancy.z(packed),
                            block, hull.containerAt(packed)));
        }
        return builder.build(hull.revision());
    }

    public record Result(boolean ok, String error, ShipTemplate template) {
        public static Result ok(ShipTemplate template) { return new Result(true, "", template); }
        public static Result fail(String error) { return new Result(false, error, null); }
        public ShipTemplate orThrow() {
            if (!ok) throw new IllegalArgumentException(error);
            return template;
        }
    }
}
