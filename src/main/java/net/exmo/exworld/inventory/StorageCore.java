package net.exmo.exworld.inventory;

/** Capacity granted by an equipped storage core. */
public final class StorageCore {
    public static final int COLUMNS = 9;
    public static final int ROWS = 6;
    public static final int GRID_CELLS = COLUMNS * ROWS;
    public static final int BACKPACK_CELLS = 27;
    public static final int EXTENSION_CELLS = 27;
    public static final int BASIC_EXTENSION = 27;

    private StorageCore() {}

    public static int unlockedCells(boolean coreEquipped) {
        return coreEquipped ? GRID_CELLS : BACKPACK_CELLS;
    }

    public static int unlockedCells(int extensionCells) {
        int extra = Math.max(0, Math.min(EXTENSION_CELLS, extensionCells));
        return BACKPACK_CELLS + extra;
    }

    public static boolean canUnequip(InventoryGrid<?> grid) {
        return grid == null || !grid.extensionOccupied();
    }
}
