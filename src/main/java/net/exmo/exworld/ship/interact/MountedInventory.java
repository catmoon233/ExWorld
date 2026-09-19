package net.exmo.exworld.ship.interact;

import net.exmo.exworld.ship.assembly.ShipMaterializer;
import net.exmo.exworld.ship.model.ShipSlot;
import net.exmo.exworld.ship.entity.ShipEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Hull-backed chest/barrel inventory. Writes non-empty slots back through one callback. */
public final class MountedInventory implements Container {
    private final ItemStack[] items;
    private final ServerLevel level;
    private final ShipEntity ship;
    private final Consumer<List<ShipSlot>> onChange;

    public MountedInventory(int size, List<ShipSlot> slots, ServerLevel level, ShipEntity ship, Consumer<List<ShipSlot>> onChange) {
        this.items = new ItemStack[Math.max(9, size)];
        java.util.Arrays.fill(this.items, ItemStack.EMPTY);
        this.level = level;
        this.ship = ship;
        this.onChange = onChange;
        for (ShipSlot slot : slots) {
            if (slot.index() >= 0 && slot.index() < items.length) items[slot.index()] = ShipMaterializer.stack(level, slot);
        }
    }

    @Override public int getContainerSize() { return items.length; }

    @Override public boolean isEmpty() {
        for (ItemStack item : items) if (!item.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.length ? items[slot] : ItemStack.EMPTY;
    }

    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack current = getItem(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = current.split(amount);
        setChanged();
        return taken;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= items.length) return ItemStack.EMPTY;
        ItemStack stack = items[slot];
        items[slot] = ItemStack.EMPTY;
        return stack;
    }

    @Override public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < items.length) items[slot] = stack;
        setChanged();
    }

    @Override public void setChanged() {
        List<ShipSlot> slots = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = items[i];
            if (stack.isEmpty()) continue;
            String extra = "";
            try {
                Tag saved = stack.save(level.registryAccess());
                extra = saved.toString();
            } catch (Exception ignored) {
            }
            slots.add(new ShipSlot(i, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), extra));
        }
        onChange.accept(slots);
    }

    @Override public boolean stillValid(Player player) {
        return ship.isAlive() && ship.getBoundingBox().inflate(8).contains(player.position());
    }
    @Override public void clearContent() { java.util.Arrays.fill(items, ItemStack.EMPTY); setChanged(); }
}
