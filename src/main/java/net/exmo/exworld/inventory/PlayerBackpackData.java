package net.exmo.exworld.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Persistent extra backpack slots: extension, weapon rails, core, accessories. */
public final class PlayerBackpackData implements Container, INBTSerializable<CompoundTag> {
    public static final int EXTENSION = StorageCore.EXTENSION_CELLS;
    public static final int WEAPON_1 = EXTENSION;
    public static final int WEAPON_2 = EXTENSION + 1;
    public static final int CORE = EXTENSION + 2;
    public static final int ACCESSORY_START = EXTENSION + 3;
    public static final int ACCESSORY_COUNT = 6;
    public static final int SIZE = ACCESSORY_START + ACCESSORY_COUNT;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private boolean grantedDefaultCore;

    public boolean grantedDefaultCore() {
        return grantedDefaultCore;
    }

    public void grantedDefaultCore(boolean value) {
        grantedDefaultCore = value;
        setChanged();
    }

    public ItemStack extension(int index) {
        if (index < 0 || index >= EXTENSION) return ItemStack.EMPTY;
        return items.get(index);
    }

    public void setExtension(int index, ItemStack stack) {
        if (index < 0 || index >= EXTENSION) return;
        items.set(index, stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack);
        setChanged();
    }

    public ItemStack weapon(int slot) {
        return slot == 0 ? items.get(WEAPON_1) : slot == 1 ? items.get(WEAPON_2) : ItemStack.EMPTY;
    }

    public void setWeapon(int slot, ItemStack stack) {
        if (slot == 0) items.set(WEAPON_1, copy(stack));
        else if (slot == 1) items.set(WEAPON_2, copy(stack));
        setChanged();
    }

    public ItemStack core() {
        return items.get(CORE);
    }

    public void setCore(ItemStack stack) {
        items.set(CORE, copy(stack));
        setChanged();
    }

    public ItemStack accessory(int index) {
        if (index < 0 || index >= ACCESSORY_COUNT) return ItemStack.EMPTY;
        return items.get(ACCESSORY_START + index);
    }

    public void setAccessory(int index, ItemStack stack) {
        if (index < 0 || index >= ACCESSORY_COUNT) return;
        items.set(ACCESSORY_START + index, copy(stack));
        setChanged();
    }

    public PlayerBackpackData copy() {
        PlayerBackpackData copy = new PlayerBackpackData();
        for (int i = 0; i < SIZE; i++) copy.items.set(i, this.items.get(i).copy());
        copy.grantedDefaultCore = grantedDefaultCore;
        return copy;
    }

    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) {
        return slot >= 0 && slot < SIZE ? items.get(slot) : ItemStack.EMPTY;
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack split = stack.split(amount);
        if (stack.isEmpty()) items.set(slot, ItemStack.EMPTY);
        setChanged();
        return split;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        if (slot >= 0 && slot < SIZE) items.set(slot, ItemStack.EMPTY);
        return stack;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SIZE) return;
        items.set(slot, copy(stack));
        setChanged();
    }
    @Override public void setChanged() {}
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < SIZE; i++) {
            if (items.get(i).isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("slot", (byte) i);
            entry.put("item", items.get(i).save(provider));
            list.add(entry);
        }
        tag.put("items", list);
        tag.putBoolean("granted_default_core", grantedDefaultCore);
        return tag;
    }

    @Override public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        items.clear();
        ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("slot") & 0xFF;
            if (slot >= SIZE) continue;
            items.set(slot, ItemStack.parse(provider, entry.get("item")).orElse(ItemStack.EMPTY));
        }
        grantedDefaultCore = tag.getBoolean("granted_default_core");
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }
}
