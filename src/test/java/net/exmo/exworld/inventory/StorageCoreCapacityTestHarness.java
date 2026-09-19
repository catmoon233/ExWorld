package net.exmo.exworld.inventory;

public final class StorageCoreCapacityTestHarness {
    public static void main(String[] args) {
        if (StorageCore.unlockedCells(false) != 27) throw new AssertionError("no core unlocks 27");
        if (StorageCore.unlockedCells(true) != 54) throw new AssertionError("basic core unlocks 54");
        if (StorageCore.unlockedCells(99) != 54) throw new AssertionError("extension is capped at 27 extra");

        InventoryGrid<String> grid = new InventoryGrid<>();
        grid.setUnlocked(StorageCore.unlockedCells(true));
        if (!StorageCore.canUnequip(grid)) throw new AssertionError("empty extension allows unequip");
        if (!grid.place(27, ItemFootprint.UNIT, "ore")) throw new AssertionError("core cells should accept items");
        if (StorageCore.canUnequip(grid)) throw new AssertionError("occupied extension must refuse unequip");
        grid.take(27);
        if (!StorageCore.canUnequip(grid)) throw new AssertionError("cleared extension allows unequip");

        InventoryGrid<String> locked = new InventoryGrid<>();
        locked.setUnlocked(StorageCore.unlockedCells(false));
        if (locked.place(30, ItemFootprint.UNIT, "blocked")) {
            throw new AssertionError("locked core region must reject items");
        }
        System.out.println("Storage core capacity tests passed");
    }
}
