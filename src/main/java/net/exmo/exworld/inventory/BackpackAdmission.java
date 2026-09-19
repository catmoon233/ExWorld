package net.exmo.exworld.inventory;

/** Pickup / reward insertion: merge first, then fit a rectangle, then 1×1 hotbar. */
public final class BackpackAdmission<T> {
    public enum Kind { ACCEPTED, REJECTED }

    public record Result<T>(Kind kind, T remaining) {
        public static <T> Result<T> accepted() {
            return new Result<>(Kind.ACCEPTED, null);
        }

        public static <T> Result<T> rejected(T remaining) {
            return new Result<>(Kind.REJECTED, remaining);
        }

        public boolean ok() {
            return kind == Kind.ACCEPTED;
        }
    }

    public static final int HOTBAR_SIZE = 9;

    private final StackOps<T> ops;

    public BackpackAdmission(StackOps<T> ops) {
        if (ops == null) throw new IllegalArgumentException("stack ops are required");
        this.ops = ops;
    }

    public Result<T> admit(InventoryGrid<T> grid, T[] hotbar, T incoming) {
        if (incoming == null || ops.isEmpty(incoming)) return Result.accepted();
        T remaining = incoming;
        remaining = stackIntoGrid(grid, remaining);
        if (ops.isEmpty(remaining)) return Result.accepted();
        if (ops.footprint(remaining).unit()) {
            remaining = stackIntoHotbar(hotbar, remaining);
            if (ops.isEmpty(remaining)) return Result.accepted();
        }
        remaining = placeInGrid(grid, remaining);
        if (ops.isEmpty(remaining)) return Result.accepted();
        if (ops.footprint(remaining).unit()) {
            remaining = placeInHotbar(hotbar, remaining);
            if (ops.isEmpty(remaining)) return Result.accepted();
        }
        return Result.rejected(remaining);
    }

    private T stackIntoGrid(InventoryGrid<T> grid, T remaining) {
        for (InventoryGrid.Placed<T> placed : grid.items()) {
            if (ops.isEmpty(remaining)) break;
            if (!ops.sameIgnoringRotation(placed.stack(), remaining)) continue;
            remaining = merge(placed.stack(), remaining, merged -> grid.setStack(placed.origin(), merged));
        }
        return remaining;
    }

    private T stackIntoHotbar(T[] hotbar, T remaining) {
        if (hotbar == null) return remaining;
        for (int i = 0; i < Math.min(HOTBAR_SIZE, hotbar.length); i++) {
            if (ops.isEmpty(remaining)) break;
            T existing = hotbar[i];
            if (existing == null || ops.isEmpty(existing) || !ops.sameIgnoringRotation(existing, remaining)) continue;
            T current = remaining;
            int index = i;
            remaining = merge(existing, remaining, merged -> hotbar[index] = merged);
            if (remaining == current) break;
        }
        return remaining;
    }

    private T placeInGrid(InventoryGrid<T> grid, T remaining) {
        ItemFootprint footprint = ops.footprint(remaining);
        int origin = grid.findOrigin(footprint);
        boolean rotated = false;
        if (origin < 0 && !footprint.square()) {
            ItemFootprint flipped = footprint.rotated();
            origin = grid.findOrigin(flipped);
            if (origin >= 0) {
                footprint = flipped;
                rotated = !ops.rotated(remaining);
            }
        }
        if (origin < 0) return remaining;
        T placed = rotated ? ops.withRotated(remaining, true) : remaining;
        grid.place(origin, footprint, placed);
        return ops.withCount(remaining, 0);
    }

    private T placeInHotbar(T[] hotbar, T remaining) {
        if (hotbar == null || !ops.footprint(remaining).unit()) return remaining;
        for (int i = 0; i < Math.min(HOTBAR_SIZE, hotbar.length); i++) {
            if (hotbar[i] == null || ops.isEmpty(hotbar[i])) {
                hotbar[i] = ops.withRotated(remaining, false);
                return ops.withCount(remaining, 0);
            }
        }
        return remaining;
    }

    private T merge(T destination, T source, java.util.function.Consumer<T> writeDestination) {
        int moved = StackingRules.transferable(ops.count(source), ops.count(destination), ops.maxCount(destination));
        if (moved <= 0) return source;
        writeDestination.accept(ops.withCount(destination, ops.count(destination) + moved));
        return ops.withCount(source, ops.count(source) - moved);
    }
}
