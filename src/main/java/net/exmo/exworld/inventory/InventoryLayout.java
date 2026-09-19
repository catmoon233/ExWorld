package net.exmo.exworld.inventory;

/** Fixed backpack geometry: 16px item cells with a 1px gap, centered by the screen. */
public final class InventoryLayout {
    public static final int ITEM = 16;
    public static final int GAP = 1;
    public static final int CELL = ITEM + GAP;
    public static final int COLUMNS = StorageCore.COLUMNS;
    public static final int ROWS = StorageCore.ROWS;
    public static final int GRID_WIDTH = COLUMNS * ITEM + (COLUMNS - 1) * GAP;
    public static final int GRID_HEIGHT = ROWS * ITEM + (ROWS - 1) * GAP;

    public static final int PAD = 12;
    public static final int TAB_H = 22;
    public static final int LEFT_WIDTH = 168;

    public static final int GRID_X = PAD + LEFT_WIDTH + PAD;
    public static final int GRID_Y = TAB_H + PAD;
    public static final int HOTBAR_Y = GRID_Y + GRID_HEIGHT + 6;

    public static final int IMAGE_WIDTH = GRID_X + GRID_WIDTH + PAD;
    public static final int IMAGE_HEIGHT = 208;

    public static final int HELMET_X = PAD;
    public static final int HELMET_Y = GRID_Y;
    public static final int CHEST_X = PAD;
    public static final int CHEST_Y = HELMET_Y + CELL;
    public static final int LEGS_X = PAD;
    public static final int LEGS_Y = CHEST_Y + CELL;
    public static final int BOOTS_X = PAD;
    public static final int BOOTS_Y = LEGS_Y + CELL;

    public static final int DOLL_X1 = PAD + ITEM + 8;
    public static final int DOLL_Y1 = GRID_Y;
    public static final int DOLL_X2 = PAD + LEFT_WIDTH - 8;
    public static final int DOLL_Y2 = DOLL_Y1 + 112;

    public static final int WEAPON_WIDTH = ITEM * 3 + GAP * 2;
    public static final int WEAPON1_X = PAD;
    public static final int WEAPON1_Y = DOLL_Y2 + 8;
    public static final int WEAPON2_X = PAD;
    public static final int WEAPON2_Y = WEAPON1_Y + CELL;
    public static final int CORE_X = PAD + WEAPON_WIDTH + 8;
    public static final int CORE_Y = WEAPON2_Y;

    public static final int ACCESSORY_X = PAD + 8;
    public static final int ACCESSORY_Y = GRID_Y + 20;
    public static final int ACCESSORY_BTN_W = 40;
    public static final int ACCESSORY_BTN_H = 14;
    public static final int ACCESSORY_BTN_X = PAD + LEFT_WIDTH - ACCESSORY_BTN_W;
    public static final int ACCESSORY_BTN_Y = GRID_Y - 2;
    public static final int EDIT_BTN_X = ACCESSORY_BTN_X;
    public static final int EDIT_BTN_Y = ACCESSORY_BTN_Y + ACCESSORY_BTN_H + 2;

    public static final int PANEL = 0xD8000000;
    public static final int SURFACE = 0xFF141414;
    public static final int SURFACE_INNER = 0xFF1C1C1C;
    public static final int SLOT = 0xFF2B2B2B;
    public static final int SLOT_LOCK = 0xFF101010;
    public static final int LINE = 0xFF3A3A3A;
    public static final int TEXT = 0xFFEDEDED;
    public static final int MUTED = 0xFF8A8A8A;
    public static final int ACCENT = 0xFFF2F2F2;

    private InventoryLayout() {}
}
