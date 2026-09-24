package net.exmo.exworld.inventory;

public final class InventoryGridTestHarness {
    public static void main(String[] args) {
        InventoryGrid<String> grid = new InventoryGrid<>();
        grid.setUnlocked(StorageCore.GRID_CELLS);
        ItemFootprint twoByTwo = ItemFootprint.of(2, 2);
        if (!grid.place(0, twoByTwo, "chest")) throw new AssertionError("2x2 should fit at origin 0");
        if (grid.empty(0) || grid.empty(1) || grid.empty(9) || grid.empty(10)) {
            throw new AssertionError("2x2 must occupy four cells");
        }
        if (grid.owner(10) != 0) throw new AssertionError("occupied cells belong to origin 0");
        if (grid.place(1, ItemFootprint.UNIT, "dirt")) throw new AssertionError("overlap must be rejected");
        if (grid.take(10) == null) throw new AssertionError("take from occupied cell removes origin");
        if (!grid.empty(0) || !grid.empty(10)) throw new AssertionError("take must clear occupancy");

        if (!grid.place(0, ItemFootprint.of(1, 3), "sword")) throw new AssertionError("1x3 should place");
        if (!grid.rotate(0)) throw new AssertionError("1x3 at column 0 row 0 should rotate to 3x1");
        if (grid.footprint(0) == null || grid.footprint(0).width() != 3) {
            throw new AssertionError("rotated sword should be 3x1, got " + grid.footprint(0));
        }
        if (!grid.place(3, ItemFootprint.of(2, 2), "box")) throw new AssertionError("2x2 beside rotated sword");
        if (grid.rotate(3)) throw new AssertionError("square rotate is a no-op/false");

        InventoryGrid<String> locked = new InventoryGrid<>();
        locked.setUnlocked(StorageCore.BACKPACK_CELLS);
        if (locked.place(27, ItemFootprint.UNIT, "extra")) {
            throw new AssertionError("locked extension must reject placement");
        }
        if (locked.findOrigin(ItemFootprint.of(9, 4)) >= 0) {
            throw new AssertionError("4-row item cannot fit in 3 unlocked rows");
        }
        if (grid.resolveOrigin(8, ItemFootprint.of(2, 1), -1) != 7) {
            throw new AssertionError("2x1 on the right edge should anchor on the cell to the left");
        }
        grid.place(0, ItemFootprint.UNIT, "block");
        if (grid.resolveOrigin(0, ItemFootprint.of(2, 1), -1) != 9) {
            throw new AssertionError("blocked 2x1 should snap to the adjacent row");
        }
        if (grid.resolveOrigin(8, ItemFootprint.of(3, 3), -1) >= 0 && grid.resolveOrigin(0, ItemFootprint.of(9, 7), -1) >= 0) {
            throw new AssertionError("oversized footprints must not invent a cell");
        }

        System.out.println("Inventory grid tests passed");
    }
}
