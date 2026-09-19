package net.exmo.exworld.ship;

import net.exmo.exworld.ship.model.PartSelection;
import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipOccupancy;
import net.exmo.exworld.ship.model.ShipPart;
import net.exmo.exworld.ship.model.ShipTemplate;
import net.exmo.exworld.ship.upgrade.ShipUpgradeRules;

import java.util.List;

/** Occupancy-mask and allow-list checks for 部位变体 swaps. */
public final class ShipUpgradeRulesTestHarness {
    public static void main(String[] args) {
        ShipHull hull = ShipHull.builder()
                .add(0, 0, 0, "minecraft:oak_planks")
                .add(1, 0, 0, "minecraft:oak_planks")
                .add(2, 0, 0, "minecraft:furnace")
                .add(2, 1, 0, "minecraft:furnace")
                .build();
        int[] engine = {ShipOccupancy.pack(2, 0, 0), ShipOccupancy.pack(2, 1, 0)};
        ShipPart part = new ShipPart("engine", "引擎", 0xFF55AAFF, engine, List.of("fast_engine"), 0.4);
        ShipTemplate template = new ShipTemplate("skiff", "小艇", hull, List.of(part), PartSelection.empty());

        ShipHull wrongShape = ShipHull.builder().add(0, 0, 0, "minecraft:blast_furnace").build();
        require(!ShipUpgradeRules.apply(template, "engine", "fast_engine", wrongShape).ok(), "shape mismatch must fail");

        ShipHull matching = ShipHull.builder()
                .add(0, 0, 0, "minecraft:blast_furnace")
                .add(0, 1, 0, "minecraft:blast_furnace")
                .build();
        require(!ShipUpgradeRules.apply(template, "engine", "not_listed", matching).ok(), "unlisted variant must fail");

        var applied = ShipUpgradeRules.apply(template, "engine", "fast_engine", matching);
        require(applied.ok(), applied.error());
        ShipHull next = applied.template().hull();
        require(next.revision() == hull.revision() + 1, "upgrade must bump revision");
        require(next.blockAt(0, 0, 0).orElse("").equals("minecraft:oak_planks"), "unrelated voxels must stay");
        require(next.blockAt(2, 0, 0).orElse("").equals("minecraft:blast_furnace"), "part voxels must be replaced");
        require(next.blockAt(2, 1, 0).orElse("").equals("minecraft:blast_furnace"), "upper part voxel must be replaced");
        require(next.size() == 4, "upgrade must not add or drop hull volume");
        require(applied.template().selection().variant("engine").equals("fast_engine"), "selection must record the variant");
        System.out.println("SHIP_UPGRADE_RULES_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
