package net.exmo.exworld.inventory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** ItemStack adapter for admission, rotation and footprint lookup. */
public final class ItemStackOps implements StackOps<ItemStack> {
    public static final ItemStackOps INSTANCE = new ItemStackOps();

    private ItemStackOps() {}

    @Override public boolean isEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }

    @Override public boolean sameIgnoringRotation(ItemStack left, ItemStack right) {
        if (isEmpty(left) || isEmpty(right)) return false;
        return ItemStack.isSameItemSameComponents(withoutRotation(left), withoutRotation(right));
    }

    @Override public int count(ItemStack stack) {
        return isEmpty(stack) ? 0 : stack.getCount();
    }

    @Override public int maxCount(ItemStack stack) {
        return isEmpty(stack) ? 0 : stack.getMaxStackSize();
    }

    @Override public ItemStack withCount(ItemStack stack, int count) {
        if (isEmpty(stack) || count <= 0) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    @Override public ItemFootprint footprint(ItemStack stack) {
        ItemFootprint base = FootprintRuleStore.current().of(id(stack));
        return InventoryRegistries.rotated(stack) ? base.rotated() : base;
    }

    @Override public ItemStack withRotated(ItemStack stack, boolean rotated) {
        return InventoryRegistries.withRotated(stack, rotated);
    }

    @Override public boolean rotated(ItemStack stack) {
        return InventoryRegistries.rotated(stack);
    }

    public static String id(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static ItemStack withoutRotation(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !InventoryRegistries.rotated(stack)) return stack;
        return InventoryRegistries.withRotated(stack, false);
    }
}
