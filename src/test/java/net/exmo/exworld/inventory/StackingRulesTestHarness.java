package net.exmo.exworld.inventory;

public final class StackingRulesTestHarness {
    public static void main(String[] args) {
        if (StackingRules.transferable(10, 54, 64) != 10) {
            throw new AssertionError("should move the whole source into remaining space");
        }
        if (StackingRules.transferable(20, 60, 64) != 4) {
            throw new AssertionError("should clamp to remaining capacity");
        }
        if (StackingRules.transferable(8, 64, 64) != 0) {
            throw new AssertionError("full stack accepts nothing");
        }
        if (StackingRules.transferable(0, 1, 64) != 0) {
            throw new AssertionError("empty source transfers nothing");
        }

        InventoryGrid<StubStack> grid = new InventoryGrid<>();
        grid.setUnlocked(StorageCore.GRID_CELLS);
        StubStackOps ops = new StubStackOps();
        BackpackAdmission<StubStack> admission = new BackpackAdmission<>(ops);
        grid.place(0, ItemFootprint.UNIT, new StubStack("stone", 60, 64, false, ItemFootprint.UNIT));
        StubStack[] hotbar = new StubStack[9];
        BackpackAdmission.Result<StubStack> stacked = admission.admit(grid, hotbar, new StubStack("stone", 10, 64, true, ItemFootprint.UNIT));
        if (!stacked.ok()) throw new AssertionError("identical items ignore rotation when stacking");
        if (ops.count(grid.stack(0)) != 64) throw new AssertionError("merged count should be 64, got " + ops.count(grid.stack(0)));

        if (grid.stack(1) == null || ops.count(grid.stack(1)) != 6) {
            throw new AssertionError("leftover after merge should occupy the next grid cell");
        }
        BackpackAdmission.Result<StubStack> overflow = admission.admit(grid, hotbar, new StubStack("stone", 8, 64, false, ItemFootprint.UNIT));
        if (!overflow.ok()) throw new AssertionError("further overflow should stack into the leftover cell");
        if (ops.count(grid.stack(1)) != 14) throw new AssertionError("grid remainder should grow to 14, got " + ops.count(grid.stack(1)));

        System.out.println("Stacking rules tests passed");
    }

    record StubStack(String id, int count, int max, boolean rotated, ItemFootprint footprint) {}

    static final class StubStackOps implements StackOps<StubStack> {
        @Override public boolean isEmpty(StubStack stack) { return stack == null || stack.count() <= 0; }
        @Override public boolean sameIgnoringRotation(StubStack left, StubStack right) {
            return left != null && right != null && left.id.equals(right.id);
        }
        @Override public int count(StubStack stack) { return stack == null ? 0 : stack.count(); }
        @Override public int maxCount(StubStack stack) { return stack == null ? 0 : stack.max(); }
        @Override public StubStack withCount(StubStack stack, int count) {
            return new StubStack(stack.id(), count, stack.max(), stack.rotated(), stack.footprint());
        }
        @Override public ItemFootprint footprint(StubStack stack) { return stack.footprint(); }
        @Override public StubStack withRotated(StubStack stack, boolean rotated) {
            return new StubStack(stack.id(), stack.count(), stack.max(), rotated, rotated ? stack.footprint().rotated() : stack.footprint());
        }
        @Override public boolean rotated(StubStack stack) { return stack != null && stack.rotated(); }
    }
}
