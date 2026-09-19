package net.exmo.exworld.inventory;

import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.content.item.StorageCoreItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

import java.util.ArrayList;
import java.util.List;

/** Player-facing backpack operations. Vanilla 9–35 hold the first 27 cells. */
public final class PlayerBackpack {
    private static final ThreadLocal<Boolean> MUTATING = ThreadLocal.withInitial(() -> false);

    private final Player player;
    private final PlayerBackpackData data;

    private PlayerBackpack(Player player) {
        this.player = player;
        this.data = player.getData(InventoryRegistries.BACKPACK);
    }

    public static PlayerBackpack of(Player player) {
        return new PlayerBackpack(player);
    }

    public static boolean mutating() {
        return Boolean.TRUE.equals(MUTATING.get());
    }

    public PlayerBackpackData data() {
        return data;
    }

    public int unlocked() {
        ItemStack core = data.core();
        if (core.getItem() instanceof StorageCoreItem item) {
            return StorageCore.unlockedCells(item.extensionCells());
        }
        return StorageCore.BACKPACK_CELLS;
    }

    public ItemStack grid(int cell) {
        if (cell < 0 || cell >= StorageCore.GRID_CELLS) return ItemStack.EMPTY;
        if (cell < StorageCore.BACKPACK_CELLS) return player.getInventory().getItem(9 + cell);
        return data.extension(cell - StorageCore.BACKPACK_CELLS);
    }

    public void setGrid(int cell, ItemStack stack) {
        if (cell < 0 || cell >= StorageCore.GRID_CELLS) return;
        ItemStack value = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack;
        if (cell < StorageCore.BACKPACK_CELLS) player.getInventory().setItem(9 + cell, value);
        else data.setExtension(cell - StorageCore.BACKPACK_CELLS, value);
    }

    public ItemStack weapon(int slot) {
        return data.weapon(slot);
    }

    public InventoryGrid<ItemStack> snapshotGrid() {
        InventoryGrid<ItemStack> grid = new InventoryGrid<>();
        grid.setUnlocked(unlocked());
        for (int i = 0; i < StorageCore.GRID_CELLS; i++) {
            ItemStack stack = grid(i);
            if (!stack.isEmpty()) grid.place(i, ItemStackOps.INSTANCE.footprint(stack), stack);
        }
        return grid;
    }

    public int ownerOf(int cell) {
        return snapshotGrid().owner(cell);
    }

    public boolean canPlace(int origin, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        InventoryGrid<ItemStack> grid = snapshotGrid();
        int ignore = grid.owner(origin);
        return grid.canPlace(origin, ItemStackOps.INSTANCE.footprint(stack), ignore);
    }

    public boolean admit(ItemStack incoming) {
        if (incoming == null || incoming.isEmpty()) return true;
        MUTATING.set(true);
        try {
            ItemStack[] hotbar = hotbarArray();
            InventoryGrid<ItemStack> grid = snapshotGrid();
            BackpackAdmission.Result<ItemStack> result =
                    new BackpackAdmission<>(ItemStackOps.INSTANCE).admit(grid, hotbar, incoming.copy());
            writeGrid(grid);
            writeHotbar(hotbar);
            if (result.ok()) {
                incoming.setCount(0);
                return true;
            }
            incoming.setCount(result.remaining().getCount());
            return false;
        } finally {
            MUTATING.set(false);
        }
    }

    public boolean rotateCell(int cell) {
        InventoryGrid<ItemStack> grid = snapshotGrid();
        int owner = grid.owner(cell);
        if (owner < 0) return false;
        ItemStack stack = grid.stack(owner);
        if (!grid.rotate(owner)) return false;
        boolean rotated = !ItemStackOps.INSTANCE.rotated(stack);
        grid.setStack(owner, ItemStackOps.INSTANCE.withRotated(stack, rotated));
        writeGrid(grid);
        return true;
    }

    public ItemStack rotateCarried(ItemStack carried) {
        if (carried == null || carried.isEmpty()) return carried;
        ItemFootprint footprint = ItemStackOps.INSTANCE.footprint(carried);
        if (footprint.square()) return carried;
        return ItemStackOps.INSTANCE.withRotated(carried, !ItemStackOps.INSTANCE.rotated(carried));
    }

    public void grantDefaultCore() {
        if (data.grantedDefaultCore()) return;
        if (data.core().isEmpty()) {
            data.setCore(new ItemStack(ExWorldContent.BASIC_STORAGE_CORE.get()));
        }
        data.grantedDefaultCore(true);
    }

    public int count(ResourceLocation itemId) {
        int total = 0;
        for (ItemStack stack : allStacks()) {
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public List<ItemStack> allStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < 9; i++) stacks.add(inventory.getItem(i));
        for (int i = 0; i < StorageCore.GRID_CELLS; i++) {
            ItemStack stack = grid(i);
            if (!stack.isEmpty()) stacks.add(stack);
        }
        stacks.add(data.weapon(0));
        stacks.add(data.weapon(1));
        for (int i = 0; i < PlayerBackpackData.ACCESSORY_COUNT; i++) stacks.add(data.accessory(i));
        for (ItemStack armor : inventory.armor) stacks.add(armor);
        stacks.add(inventory.offhand.getFirst());
        return stacks;
    }

    public List<ItemStack> dropExtras(boolean keepInventory) {
        List<ItemStack> drops = new ArrayList<>();
        if (keepInventory) return drops;
        for (int i = 0; i < PlayerBackpackData.SIZE; i++) {
            ItemStack stack = data.getItem(i);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
                data.setItem(i, ItemStack.EMPTY);
            }
        }
        return drops;
    }

    public void copyFrom(Player original) {
        PlayerBackpackData source = original.getData(InventoryRegistries.BACKPACK);
        PlayerBackpackData target = player.getData(InventoryRegistries.BACKPACK);
        PlayerBackpackData copy = source.copy();
        for (int i = 0; i < PlayerBackpackData.SIZE; i++) target.setItem(i, copy.getItem(i));
        target.grantedDefaultCore(copy.grantedDefaultCore());
    }

    public boolean canUnequipCore() {
        InventoryGrid<ItemStack> grid = snapshotGrid();
        grid.setUnlocked(StorageCore.GRID_CELLS);
        return StorageCore.canUnequip(grid);
    }

    public static boolean open(ServerPlayer player) {
        if (player == null) return false;
        of(player).grantDefaultCore();
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> new PlayerBackpackMenu(id, inv),
                net.minecraft.network.chat.Component.translatable("screen.exworld.backpack")));
        return true;
    }

    private ItemStack[] hotbarArray() {
        ItemStack[] hotbar = new ItemStack[9];
        for (int i = 0; i < 9; i++) hotbar[i] = player.getInventory().getItem(i);
        return hotbar;
    }

    private void writeHotbar(ItemStack[] hotbar) {
        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, hotbar[i] == null ? ItemStack.EMPTY : hotbar[i]);
        }
    }

    private void writeGrid(InventoryGrid<ItemStack> grid) {
        for (int i = 0; i < StorageCore.GRID_CELLS; i++) setGrid(i, ItemStack.EMPTY);
        for (InventoryGrid.Placed<ItemStack> placed : grid.items()) {
            setGrid(placed.origin(), placed.stack());
        }
    }
}
