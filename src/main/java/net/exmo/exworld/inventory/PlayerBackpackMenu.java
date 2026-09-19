package net.exmo.exworld.inventory;

import net.exmo.exworld.content.item.StorageCoreItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;

public final class PlayerBackpackMenu extends AbstractContainerMenu {
    public static final int GRID_SLOTS = StorageCore.GRID_CELLS;
    public static final int HOTBAR_START = GRID_SLOTS;
    public static final int ARMOR_START = HOTBAR_START + 9;
    public static final int WEAPON_START = ARMOR_START + 4;
    public static final int CORE_SLOT = WEAPON_START + 2;
    public static final int ACCESSORY_START = CORE_SLOT + 1;

    private final Player player;
    private final PlayerBackpack backpack;
    private final GridContainer grid;

    public PlayerBackpackMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory);
    }

    public PlayerBackpackMenu(int id, Inventory inventory) {
        super(InventoryRegistries.PLAYER_BACKPACK.get(), id);
        this.player = inventory.player;
        this.backpack = PlayerBackpack.of(player);
        this.grid = new GridContainer(backpack);
        backpack.grantDefaultCore();
        addGridSlots();
        addHotbarSlots(inventory);
        addArmorSlots(inventory);
        addEquipmentSlots();
    }

    public PlayerBackpack backpack() {
        return backpack;
    }

    public boolean accessoryPage() {
        return FMLEnvironment.dist.isClient() && BackpackUi.accessories;
    }

    public boolean equipmentPage() {
        return !FMLEnvironment.dist.isClient() || !BackpackUi.accessories;
    }

    private void addGridSlots() {
        for (int row = 0; row < StorageCore.ROWS; row++) {
            for (int col = 0; col < StorageCore.COLUMNS; col++) {
                int cell = row * StorageCore.COLUMNS + col;
                addSlot(new GridSlot(grid, cell,
                        InventoryLayout.GRID_X + col * InventoryLayout.CELL,
                        InventoryLayout.GRID_Y + row * InventoryLayout.CELL));
            }
        }
    }

    private void addHotbarSlots(Inventory inventory) {
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i,
                    InventoryLayout.GRID_X + i * InventoryLayout.CELL,
                    InventoryLayout.HOTBAR_Y) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return ItemStackOps.INSTANCE.footprint(stack).unit();
                }
            });
        }
    }

    private void addArmorSlots(Inventory inventory) {
        addSlot(armor(inventory, EquipmentSlot.HEAD, 39, InventoryLayout.HELMET_X, InventoryLayout.HELMET_Y));
        addSlot(armor(inventory, EquipmentSlot.CHEST, 38, InventoryLayout.CHEST_X, InventoryLayout.CHEST_Y));
        addSlot(armor(inventory, EquipmentSlot.LEGS, 37, InventoryLayout.LEGS_X, InventoryLayout.LEGS_Y));
        addSlot(armor(inventory, EquipmentSlot.FEET, 36, InventoryLayout.BOOTS_X, InventoryLayout.BOOTS_Y));
    }

    private Slot armor(Inventory inventory, EquipmentSlot slot, int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == slot
                        || player.getEquipmentSlotForItem(stack) == slot;
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean isActive() { return equipmentPage(); }
        };
    }

    private void addEquipmentSlots() {
        PlayerBackpackData data = backpack.data();
                addSlot(new Slot(data, PlayerBackpackData.WEAPON_1, InventoryLayout.WEAPON1_X, InventoryLayout.WEAPON1_Y) {
            @Override public boolean isActive() { return equipmentPage(); }
        });
                addSlot(new Slot(data, PlayerBackpackData.WEAPON_2, InventoryLayout.WEAPON2_X, InventoryLayout.WEAPON2_Y) {
            @Override public boolean isActive() { return equipmentPage(); }
        });
        addSlot(new Slot(data, PlayerBackpackData.CORE, InventoryLayout.CORE_X, InventoryLayout.CORE_Y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof StorageCoreItem;
            }
            @Override public boolean mayPickup(Player player) {
                return backpack.canUnequipCore();
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean isActive() { return equipmentPage(); }
        });
        for (int i = 0; i < PlayerBackpackData.ACCESSORY_COUNT; i++) {
            int col = i % 3;
            int row = i / 3;
            addSlot(new Slot(data, PlayerBackpackData.ACCESSORY_START + i,
                    InventoryLayout.ACCESSORY_X + col * InventoryLayout.CELL,
                    InventoryLayout.ACCESSORY_Y + row * InventoryLayout.CELL) {
                @Override public boolean isActive() { return accessoryPage(); }
            });
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < GRID_SLOTS) {
            int owner = backpack.ownerOf(slotId);
            if (owner >= 0 && owner != slotId) slotId = owner;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < GRID_SLOTS) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_START + 9, false)) return ItemStack.EMPTY;
        } else if (index < ARMOR_START) {
            if (!moveIntoGrid(stack)) return ItemStack.EMPTY;
        } else {
            if (!moveIntoGrid(stack) && !moveItemStackTo(stack, HOTBAR_START, HOTBAR_START + 9, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    private boolean moveIntoGrid(ItemStack stack) {
        ItemStack moving = stack.copy();
        boolean accepted = backpack.admit(moving);
        stack.setCount(moving.getCount());
        broadcastChanges();
        return accepted || stack.getCount() < moving.getCount() + stack.getCount();
    }

    @Override public boolean stillValid(Player player) {
        return this.player == player;
    }

    public void rotate(int slotId) {
        if (slotId < 0) {
            setCarried(backpack.rotateCarried(getCarried()));
            return;
        }
        if (slotId < GRID_SLOTS) backpack.rotateCell(slotId);
        broadcastChanges();
    }

    private final class GridSlot extends Slot {
        private final int cell;

        private GridSlot(Container container, int cell, int x, int y) {
            super(container, cell, x, y);
            this.cell = cell;
        }

        @Override public boolean mayPlace(ItemStack stack) {
            if (cell >= backpack.unlocked()) return false;
            int owner = backpack.ownerOf(cell);
            if (owner >= 0 && owner != cell) return false;
            return backpack.canPlace(cell, stack);
        }

        @Override public boolean isActive() {
            return cell < backpack.unlocked();
        }

        @Override public boolean mayPickup(Player player) {
            int owner = backpack.ownerOf(cell);
            return owner < 0 || owner == cell;
        }
    }

    private static final class GridContainer implements Container {
        private final PlayerBackpack backpack;

        private GridContainer(PlayerBackpack backpack) {
            this.backpack = backpack;
        }

        @Override public int getContainerSize() { return StorageCore.GRID_CELLS; }
        @Override public boolean isEmpty() {
            for (int i = 0; i < StorageCore.GRID_CELLS; i++) if (!backpack.grid(i).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot) { return backpack.grid(slot); }
        @Override public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = backpack.grid(slot);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack split = stack.split(amount);
            backpack.setGrid(slot, stack);
            return split;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = backpack.grid(slot);
            backpack.setGrid(slot, ItemStack.EMPTY);
            return stack;
        }
        @Override public void setItem(int slot, ItemStack stack) { backpack.setGrid(slot, stack); }
        @Override public void setChanged() {}
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() {
            for (int i = 0; i < StorageCore.GRID_CELLS; i++) backpack.setGrid(i, ItemStack.EMPTY);
        }
    }
}
