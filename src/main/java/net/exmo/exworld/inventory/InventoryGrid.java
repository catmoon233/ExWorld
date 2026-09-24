package net.exmo.exworld.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rectangular backpack occupancy. Multi-cell items store the stack at the origin
 * and mark the remaining cells as owned by that origin.
 */
public final class InventoryGrid<T> {
    public static final int COLUMNS = StorageCore.COLUMNS;
    public static final int ROWS = StorageCore.ROWS;
    public static final int CELLS = StorageCore.GRID_CELLS;

    private int unlocked = StorageCore.BACKPACK_CELLS;
    private final Object[] stacks = new Object[CELLS];
    private final ItemFootprint[] footprints = new ItemFootprint[CELLS];
    private final int[] owner = new int[CELLS];

    public InventoryGrid() {
        Arrays.fill(owner, -1);
    }

    public void setUnlocked(int unlocked) {
        this.unlocked = Math.max(0, Math.min(CELLS, unlocked));
    }

    public int unlocked() {
        return unlocked;
    }

    public static int index(int x, int y) {
        return y * COLUMNS + x;
    }

    public static int x(int cell) {
        return cell % COLUMNS;
    }

    public static int y(int cell) {
        return cell / COLUMNS;
    }

    public boolean unlocked(int cell) {
        return cell >= 0 && cell < unlocked;
    }

    public boolean extensionOccupied() {
        for (int i = StorageCore.BACKPACK_CELLS; i < CELLS; i++) {
            if (owner[i] >= 0) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public T stack(int origin) {
        return (T) stacks[origin];
    }

    public ItemFootprint footprint(int origin) {
        return footprints[origin];
    }

    public int owner(int cell) {
        if (cell < 0 || cell >= CELLS) return -1;
        return owner[cell];
    }

    public boolean empty(int cell) {
        return owner(cell) < 0;
    }

    public boolean origin(int cell) {
        return cell >= 0 && cell < CELLS && footprints[cell] != null;
    }

    public boolean canPlace(int origin, ItemFootprint footprint) {
        return canPlace(origin, footprint, -1);
    }

    public boolean canPlace(int origin, ItemFootprint footprint, int ignoreOwner) {
        if (footprint == null || origin < 0 || origin >= CELLS) return false;
        int ox = x(origin);
        int oy = y(origin);
        if (ox + footprint.width() > COLUMNS || oy + footprint.height() > ROWS) return false;
        for (int dy = 0; dy < footprint.height(); dy++) {
            for (int dx = 0; dx < footprint.width(); dx++) {
                int cell = index(ox + dx, oy + dy);
                if (!unlocked(cell)) return false;
                int current = owner[cell];
                if (current >= 0 && current != ignoreOwner) return false;
            }
        }
        return true;
    }

    public boolean place(int origin, ItemFootprint footprint, T stack) {
        if (stack == null || !canPlace(origin, footprint)) return false;
        occupy(origin, footprint, stack);
        return true;
    }

    public boolean replace(int origin, ItemFootprint footprint, T stack) {
        int existing = owner[origin] >= 0 ? owner[origin] : origin;
        if (stack == null || !canPlace(origin, footprint, existing)) return false;
        if (owner[existing] >= 0) clear(existing);
        occupy(origin, footprint, stack);
        return true;
    }

    public T take(int cell) {
        int origin = owner(cell);
        if (origin < 0) return null;
        T stack = stack(origin);
        clear(origin);
        return stack;
    }

    public boolean rotate(int cell) {
        int origin = owner(cell);
        if (origin < 0) return false;
        ItemFootprint footprint = footprints[origin];
        if (footprint == null || footprint.square()) return false;
        ItemFootprint rotated = footprint.rotated();
        T stack = stack(origin);
        if (!canPlace(origin, rotated, origin)) return false;
        occupy(origin, rotated, stack);
        return true;
    }

    public int findOrigin(ItemFootprint footprint) {
        if (footprint == null) return -1;
        for (int i = 0; i < unlocked; i++) {
            if (canPlace(i, footprint)) return i;
        }
        return -1;
    }


    /**
     * Origin used when dropping a multi-cell item. The clicked cell may be any cell of the
     * footprint, not only the top-left. If nothing covers that cell, the nearest adjacent
     * origin that fits is used.
     */
    public int resolveOrigin(int cell, ItemFootprint footprint, int ignoreOwner) {
        if (footprint == null || cell < 0 || cell >= CELLS) return -1;
        if (canPlace(cell, footprint, ignoreOwner)) return cell;

        int cx = x(cell);
        int cy = y(cell);
        int covering = nearestCoveringOrigin(cx, cy, footprint, ignoreOwner);
        if (covering >= 0) return covering;
        return nearestAdjacentOrigin(cx, cy, footprint, ignoreOwner);
    }

    private int nearestCoveringOrigin(int cx, int cy, ItemFootprint footprint, int ignoreOwner) {
        int best = -1;
        int bestScore = Integer.MAX_VALUE;
        for (int dy = 0; dy < footprint.height(); dy++) {
            for (int dx = 0; dx < footprint.width(); dx++) {
                if (dx == 0 && dy == 0) continue;
                int ox = cx - dx;
                int oy = cy - dy;
                if (ox < 0 || oy < 0 || ox >= COLUMNS || oy >= ROWS) continue;
                int origin = index(ox, oy);
                if (!canPlace(origin, footprint, ignoreOwner)) continue;
                int score = dx + dy;
                if (score < bestScore) {
                    bestScore = score;
                    best = origin;
                }
            }
        }
        return best;
    }

    private int nearestAdjacentOrigin(int cx, int cy, ItemFootprint footprint, int ignoreOwner) {
        int best = -1;
        int bestDist = Integer.MAX_VALUE;
        for (int oy = Math.max(0, cy - 1); oy <= Math.min(ROWS - 1, cy + 1); oy++) {
            for (int ox = Math.max(0, cx - 1); ox <= Math.min(COLUMNS - 1, cx + 1); ox++) {
                if (ox == cx && oy == cy) continue;
                int origin = index(ox, oy);
                if (!canPlace(origin, footprint, ignoreOwner)) continue;
                int dist = Math.abs(ox - cx) + Math.abs(oy - cy);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = origin;
                }
            }
        }
        return best;
    }


    public List<Placed<T>> items() {
        List<Placed<T>> result = new ArrayList<>();
        for (int i = 0; i < CELLS; i++) {
            if (footprints[i] != null && stacks[i] != null) {
                result.add(new Placed<>(i, footprints[i], stack(i)));
            }
        }
        return result;
    }

    public void setStack(int origin, T stack) {
        if (!origin(origin)) return;
        stacks[origin] = stack;
    }

    public record Placed<T>(int origin, ItemFootprint footprint, T stack) {}

    private void occupy(int origin, ItemFootprint footprint, T stack) {
        if (footprints[origin] != null) clearOccupancy(origin);
        int ox = x(origin);
        int oy = y(origin);
        for (int dy = 0; dy < footprint.height(); dy++) {
            for (int dx = 0; dx < footprint.width(); dx++) {
                owner[index(ox + dx, oy + dy)] = origin;
            }
        }
        stacks[origin] = stack;
        footprints[origin] = footprint;
    }

    private void clear(int origin) {
        clearOccupancy(origin);
        stacks[origin] = null;
        footprints[origin] = null;
    }

    private void clearOccupancy(int origin) {
        ItemFootprint footprint = footprints[origin];
        if (footprint == null) {
            if (owner[origin] == origin) owner[origin] = -1;
            return;
        }
        int ox = x(origin);
        int oy = y(origin);
        for (int dy = 0; dy < footprint.height(); dy++) {
            for (int dx = 0; dx < footprint.width(); dx++) {
                int cell = index(ox + dx, oy + dy);
                if (owner[cell] == origin) owner[cell] = -1;
            }
        }
    }
}
