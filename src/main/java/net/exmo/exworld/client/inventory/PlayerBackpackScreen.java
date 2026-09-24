package net.exmo.exworld.client.inventory;

import net.exmo.exmodifier.api.ExModifierApi;
import net.exmo.exworld.client.tooltip.QualityChrome;
import net.exmo.exworld.client.tooltip.RarityPalette;
import net.exmo.exworld.inventory.BackpackUi;
import net.exmo.exworld.inventory.InventoryLayout;
import net.exmo.exworld.inventory.ItemFootprint;
import net.exmo.exworld.inventory.ItemStackOps;
import net.exmo.exworld.inventory.PlayerBackpackMenu;
import net.exmo.exworld.inventory.StorageCore;
import net.exmo.exworld.network.InventoryPayloads;
import net.exmo.exworld.client.inventory.BackpackTabs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

public final class PlayerBackpackScreen extends AbstractContainerScreen<PlayerBackpackMenu> {
    private static final int ICON = 16;

    private float xMouse;
    private float yMouse;

    public PlayerBackpackScreen(PlayerBackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = InventoryLayout.IMAGE_WIDTH;
        this.imageHeight = InventoryLayout.IMAGE_HEIGHT;
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
        BackpackUi.accessories = false;
    }

    @Override
    protected void init() {
        super.init();
        BackpackUi.openTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.xMouse = mouseX;
        this.yMouse = mouseY;
        super.render(graphics, mouseX, mouseY, partialTick);
        drawControlLayer(graphics, mouseX, mouseY);
        renderPlacementPreview(graphics, mouseX, mouseY);
        renderCarriedLarge(graphics, mouseX, mouseY);
    }


    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, InventoryLayout.PANEL);
        renderBg(graphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        long now = System.currentTimeMillis();
        float fade = Mth.clamp((now - BackpackUi.openTime) / 180.0F, 0.0F, 1.0F);
        int fadeA = Math.round(fade * 255);

        fillAlpha(graphics, x, y + InventoryLayout.TAB_H, x + imageWidth, y + imageHeight, InventoryLayout.SURFACE, fadeA);
        fillAlpha(graphics, x, y + InventoryLayout.TAB_H, x + imageWidth, y + InventoryLayout.TAB_H + 1, InventoryLayout.LINE, fadeA);
        fillAlpha(graphics, x, y + imageHeight - 1, x + imageWidth, y + imageHeight, InventoryLayout.LINE, fadeA);
        fillAlpha(graphics, x, y + InventoryLayout.TAB_H, x + 1, y + imageHeight, InventoryLayout.LINE, fadeA);
        fillAlpha(graphics, x + imageWidth - 1, y + InventoryLayout.TAB_H, x + imageWidth, y + imageHeight, InventoryLayout.LINE, fadeA);
        int split = x + InventoryLayout.GRID_X - InventoryLayout.PAD / 2;
        fillAlpha(graphics, split, y + InventoryLayout.TAB_H + 8, split + 1, y + imageHeight - 8, InventoryLayout.LINE_INNER, fadeA);

        drawTab(graphics, x, y, Component.translatable("screen.exworld.backpack_tab"), true);
        drawTab(graphics, x + 64, y, Component.translatable("screen.exworld.character_tab"), false);
        drawTab(graphics, x + 128, y, Component.translatable("screen.exworld.quests_tab"), false);
        if (BackpackTabs.hasSequence()) {
            boolean sequenceHover = mouseX >= x + 192 && mouseX < x + 252 && mouseY >= y && mouseY < y + InventoryLayout.TAB_H;
            drawTab(graphics, x + 192, y, Component.translatable("screen.exworld.sequence_tab"), false, sequenceHover);
        }

        boolean accessories = BackpackUi.accessories;
        int hovered = hoveredGridCell(mouseX, mouseY);
        int unlocked = menu.backpack().unlocked();

        if (!accessories) {

            fillAlpha(graphics,
                    x + InventoryLayout.DOLL_X1, y + InventoryLayout.DOLL_Y1,
                    x + InventoryLayout.DOLL_X2, y + InventoryLayout.DOLL_Y2,
                    InventoryLayout.SURFACE_INNER, fadeA);
            if (minecraft != null && minecraft.player != null) {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        graphics,
                        x + InventoryLayout.DOLL_X1 + 2,
                        y + InventoryLayout.DOLL_Y1 + 2,
                        x + InventoryLayout.DOLL_X2 - 2,
                        y + InventoryLayout.DOLL_Y2 - 2,
                        30,
                        0.0625F,
                        xMouse,
                        yMouse,
                        minecraft.player
                );
            }
            if (!net.exmo.exworld.Config.decryptionMode) {
                drawFramed(graphics, x + InventoryLayout.WEAPON1_X, y + InventoryLayout.WEAPON1_Y,
                        InventoryLayout.WEAPON_WIDTH, InventoryLayout.ITEM, menu.backpack().weapon(0),
                        hoverRect(mouseX, mouseY, x + InventoryLayout.WEAPON1_X, y + InventoryLayout.WEAPON1_Y, InventoryLayout.WEAPON_WIDTH, InventoryLayout.ITEM));
                drawFramed(graphics, x + InventoryLayout.WEAPON2_X, y + InventoryLayout.WEAPON2_Y,
                        InventoryLayout.WEAPON_WIDTH, InventoryLayout.ITEM, menu.backpack().weapon(1),
                        hoverRect(mouseX, mouseY, x + InventoryLayout.WEAPON2_X, y + InventoryLayout.WEAPON2_Y, InventoryLayout.WEAPON_WIDTH, InventoryLayout.ITEM));
            }
            drawFramed(graphics, x + InventoryLayout.HELMET_X, y + InventoryLayout.HELMET_Y,
                    InventoryLayout.ITEM, InventoryLayout.ITEM, menu.slots.get(PlayerBackpackMenu.ARMOR_START).getItem(),
                    hoverRect(mouseX, mouseY, x + InventoryLayout.HELMET_X, y + InventoryLayout.HELMET_Y, InventoryLayout.ITEM, InventoryLayout.ITEM));
            drawFramed(graphics, x + InventoryLayout.CHEST_X, y + InventoryLayout.CHEST_Y,
                    InventoryLayout.ITEM, InventoryLayout.ITEM, menu.slots.get(PlayerBackpackMenu.ARMOR_START + 1).getItem(),
                    hoverRect(mouseX, mouseY, x + InventoryLayout.CHEST_X, y + InventoryLayout.CHEST_Y, InventoryLayout.ITEM, InventoryLayout.ITEM));
            drawFramed(graphics, x + InventoryLayout.LEGS_X, y + InventoryLayout.LEGS_Y,
                    InventoryLayout.ITEM, InventoryLayout.ITEM, menu.slots.get(PlayerBackpackMenu.ARMOR_START + 2).getItem(),
                    hoverRect(mouseX, mouseY, x + InventoryLayout.LEGS_X, y + InventoryLayout.LEGS_Y, InventoryLayout.ITEM, InventoryLayout.ITEM));
            drawFramed(graphics, x + InventoryLayout.BOOTS_X, y + InventoryLayout.BOOTS_Y,
                    InventoryLayout.ITEM, InventoryLayout.ITEM, menu.slots.get(PlayerBackpackMenu.ARMOR_START + 3).getItem(),
                    hoverRect(mouseX, mouseY, x + InventoryLayout.BOOTS_X, y + InventoryLayout.BOOTS_Y, InventoryLayout.ITEM, InventoryLayout.ITEM));
            drawFramed(graphics, x + InventoryLayout.CORE_X, y + InventoryLayout.CORE_Y,
                    InventoryLayout.ITEM, InventoryLayout.ITEM, menu.backpack().data().core(),
                    hoverRect(mouseX, mouseY, x + InventoryLayout.CORE_X, y + InventoryLayout.CORE_Y, InventoryLayout.ITEM, InventoryLayout.ITEM));
        } else {
            drawPlate(graphics, x + InventoryLayout.ACCESSORY_X, y + InventoryLayout.ACCESSORY_Y, 3, 2);
            for (int i = 0; i < 6; i++) {
                int ax = x + InventoryLayout.ACCESSORY_X + (i % 3) * InventoryLayout.CELL;
                int ay = y + InventoryLayout.ACCESSORY_Y + (i / 3) * InventoryLayout.CELL;
                drawFill(graphics, ax, ay, InventoryLayout.ITEM, InventoryLayout.ITEM,
                        menu.backpack().data().accessory(i), false,
                        hoverRect(mouseX, mouseY, ax, ay, InventoryLayout.ITEM, InventoryLayout.ITEM));
            }
        }

        drawUnlockedPlate(graphics, x + InventoryLayout.GRID_X, y + InventoryLayout.GRID_Y, unlocked);
        int shown = Math.max(0, Math.min(StorageCore.GRID_CELLS, unlocked));
        for (int cell = 0; cell < shown; cell++) {
            int owner = menu.backpack().ownerOf(cell);
            if (owner >= 0 && owner != cell) continue;
            ItemStack stack = menu.backpack().grid(cell);
            ItemFootprint footprint = stack.isEmpty() ? ItemFootprint.UNIT : ItemStackOps.INSTANCE.footprint(stack);
            int gx = x + gridX(cell);
            int gy = y + gridY(cell);
            int w = footprint.width() * InventoryLayout.CELL - InventoryLayout.GAP;
            int h = footprint.height() * InventoryLayout.CELL - InventoryLayout.GAP;
            drawFill(graphics, gx, gy, w, h, stack, false, covers(cell, footprint, hovered));
        }

        drawPlate(graphics, x + InventoryLayout.GRID_X, y + InventoryLayout.HOTBAR_Y, 9, 1);
        for (int i = 0; i < 9; i++) {
            int hx = x + InventoryLayout.GRID_X + i * InventoryLayout.CELL;
            drawFill(graphics, hx, y + InventoryLayout.HOTBAR_Y, InventoryLayout.ITEM, InventoryLayout.ITEM,
                    menu.slots.get(PlayerBackpackMenu.HOTBAR_START + i).getItem(), false,
                    hoverRect(mouseX, mouseY, hx, y + InventoryLayout.HOTBAR_Y, InventoryLayout.CELL, InventoryLayout.CELL));
        }
        renderPlacementGhost(graphics, mouseX, mouseY);
    }

    /** Gray outer frame, 1px inner hairline left as the plate color between cells. */
    private static void drawPlate(GuiGraphics graphics, int x, int y, int columns, int rows) {
        int w = columns * InventoryLayout.CELL - InventoryLayout.GAP;
        int h = rows * InventoryLayout.CELL - InventoryLayout.GAP;
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, InventoryLayout.LINE);
        graphics.fill(x, y, x + w, y + h, InventoryLayout.LINE_INNER);
    }

    /** Only the unlocked rectangle. Locked extension cells are not drawn. */
    private static void drawUnlockedPlate(GuiGraphics graphics, int x, int y, int unlocked) {
        int shown = Math.max(0, Math.min(StorageCore.GRID_CELLS, unlocked));
        if (shown <= 0) return;
        int fullRows = shown / InventoryLayout.COLUMNS;
        int extra = shown % InventoryLayout.COLUMNS;
        if (fullRows > 0) drawPlate(graphics, x, y, InventoryLayout.COLUMNS, fullRows);
        if (extra > 0) drawPlate(graphics, x, y + fullRows * InventoryLayout.CELL, extra, 1);
    }

    /** Accessory and edit controls sit above the doll, not under it. */
    private void drawControlLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 500);
        int bx = leftPos + InventoryLayout.ACCESSORY_BTN_X;
        int by = topPos + InventoryLayout.ACCESSORY_BTN_Y;
        int bw = InventoryLayout.ACCESSORY_BTN_W;
        int bh = InventoryLayout.ACCESSORY_BTN_H;
        boolean edit = minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
        int clusterH = edit ? InventoryLayout.EDIT_BTN_Y - InventoryLayout.ACCESSORY_BTN_Y + bh : bh;
        graphics.fill(bx - 2, by - 2, bx + bw + 2, by + clusterH + 2, InventoryLayout.LINE);
        graphics.fill(bx - 1, by - 1, bx + bw + 1, by + clusterH + 1, InventoryLayout.SURFACE);
        drawButton(graphics, bx, by, bw, bh,
                Component.translatable(BackpackUi.accessories ? "screen.exworld.backpack_equipment" : "screen.exworld.backpack_accessories"),
                mouseX, mouseY);
        if (edit) {
            drawButton(graphics, leftPos + InventoryLayout.EDIT_BTN_X, topPos + InventoryLayout.EDIT_BTN_Y, bw, bh,
                    Component.translatable("screen.exworld.footprint_edit"), mouseX, mouseY);
        }
        pose.popPose();
    }


    private void renderPlacementGhost(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return;
        int hovered = hoveredGridCell(mouseX, mouseY);
        if (hovered < 0 || menu.backpack().ownerOf(hovered) >= 0) return;
        ItemFootprint footprint = ItemStackOps.INSTANCE.footprint(carried);
        int origin = menu.backpack().resolvePlacement(hovered, carried);
        int cell = origin >= 0 ? origin : hovered;
        int w = (origin >= 0 ? footprint.width() : 1) * InventoryLayout.CELL - InventoryLayout.GAP;
        int h = (origin >= 0 ? footprint.height() : 1) * InventoryLayout.CELL - InventoryLayout.GAP;
        int gx = leftPos + gridX(cell);
        int gy = topPos + gridY(cell);
        graphics.fill(gx, gy, gx + w, gy + h, origin >= 0 ? 0x66E4EDF6 : 0x66C45C5C);
    }

    private void renderPlacementPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return;
        int hovered = hoveredGridCell(mouseX, mouseY);
        if (hovered < 0 || menu.backpack().ownerOf(hovered) >= 0) return;
        int origin = menu.backpack().resolvePlacement(hovered, carried);
        if (origin < 0) return;
        ItemFootprint footprint = ItemStackOps.INSTANCE.footprint(carried);
        renderScaledItem(graphics, carried,
                leftPos + gridX(origin),
                topPos + gridY(origin),
                footprint.width() * InventoryLayout.CELL - InventoryLayout.GAP,
                footprint.height() * InventoryLayout.CELL - InventoryLayout.GAP,
                footprint.width(),
                footprint.height());
    }

    private void renderCarriedLarge(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return;
        ItemFootprint footprint = ItemStackOps.INSTANCE.footprint(carried);
        if (footprint.unit()) return;
        int iconW = ICON * footprint.width();
        int iconH = ICON * footprint.height();
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 400);
        renderScaledItem(graphics, carried, mouseX - iconW / 2, mouseY - iconH / 2, iconW, iconH, footprint.width(), footprint.height());
        pose.popPose();
    }

    private int hoveredGridCell(int mouseX, int mouseY) {
        int gx = mouseX - leftPos - InventoryLayout.GRID_X;
        int gy = mouseY - topPos - InventoryLayout.GRID_Y;
        if (gx < 0 || gy < 0) return -1;
        int col = gx / InventoryLayout.CELL;
        int row = gy / InventoryLayout.CELL;
        if (col >= InventoryLayout.COLUMNS || row >= InventoryLayout.ROWS) return -1;
        if (gx >= InventoryLayout.GRID_WIDTH || gy >= InventoryLayout.GRID_HEIGHT) return -1;
        int cell = row * InventoryLayout.COLUMNS + col;
        return cell < menu.backpack().unlocked() ? cell : -1;

    }

    private static boolean hoverRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static boolean covers(int origin, ItemFootprint footprint, int cell) {
        if (cell < 0 || origin < 0 || footprint == null) return false;
        int ox = origin % InventoryLayout.COLUMNS;
        int oy = origin / InventoryLayout.COLUMNS;
        int cx = cell % InventoryLayout.COLUMNS;
        int cy = cell / InventoryLayout.COLUMNS;
        return cx >= ox && cx < ox + footprint.width() && cy >= oy && cy < oy + footprint.height();
    }

    private static int gridX(int cell) {
        return InventoryLayout.GRID_X + (cell % InventoryLayout.COLUMNS) * InventoryLayout.CELL;
    }

    private static int gridY(int cell) {
        return InventoryLayout.GRID_Y + (cell / InventoryLayout.COLUMNS) * InventoryLayout.CELL;
    }

    protected boolean isHovering(Slot slot, double mouseX, double mouseY) {
        int w = hitWidth(slot);
        int h = hitHeight(slot);
        return mouseX >= leftPos + slot.x && mouseX < leftPos + slot.x + w
                && mouseY >= topPos + slot.y && mouseY < topPos + slot.y + h;
    }

    private static int hitWidth(Slot slot) {
        if (slot.index >= PlayerBackpackMenu.WEAPON_START && slot.index < PlayerBackpackMenu.CORE_SLOT) {
            return InventoryLayout.WEAPON_WIDTH;
        }
        if (slot.index < PlayerBackpackMenu.HOTBAR_START + 9 || slot.index >= PlayerBackpackMenu.ACCESSORY_START) {
            return InventoryLayout.CELL;
        }
        return InventoryLayout.ITEM;
    }


    private static int hitHeight(Slot slot) {
        if (slot.index < PlayerBackpackMenu.ARMOR_START + 4 || slot.index >= PlayerBackpackMenu.ACCESSORY_START) {
            return InventoryLayout.CELL;
        }
        return InventoryLayout.ITEM;
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!menu.getCarried().isEmpty() || hoveredSlot == null) return;
        ItemStack stack = tooltipStack(hoveredSlot);
        if (!stack.isEmpty()) graphics.renderTooltip(font, stack, mouseX, mouseY);
    }

    private ItemStack tooltipStack(Slot slot) {
        if (slot.index < PlayerBackpackMenu.GRID_SLOTS) {
            int owner = menu.backpack().ownerOf(slot.index);
            return owner >= 0 ? menu.backpack().grid(owner) : ItemStack.EMPTY;
        }
        return slot.getItem();
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (!slot.isActive()) return;
        if (slot.index >= PlayerBackpackMenu.WEAPON_START && slot.index < PlayerBackpackMenu.CORE_SLOT) {
            renderWeaponRail(graphics, slot);
            return;
        }
        if (slot.index < PlayerBackpackMenu.GRID_SLOTS) {
            renderGridItem(graphics, slot);
            return;
        }
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return;
        int pad = (InventoryLayout.ITEM - ICON) / 2;
        graphics.renderItem(stack, slot.x + pad, slot.y + pad);
        graphics.renderItemDecorations(font, stack, slot.x + pad, slot.y + pad);
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        // Hover is painted with the cell fill so the highlight matches the full cell, not a 16px corner.
    }

    private void renderGridItem(GuiGraphics graphics, Slot slot) {
        int owner = menu.backpack().ownerOf(slot.index);
        if (owner >= 0 && owner != slot.index) return;
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return;
        ItemFootprint footprint = ItemStackOps.INSTANCE.footprint(stack);
        renderScaledItem(graphics, stack, slot.x, slot.y,
                footprint.width() * InventoryLayout.CELL - InventoryLayout.GAP,
                footprint.height() * InventoryLayout.CELL - InventoryLayout.GAP,
                footprint.width(),
                footprint.height());
    }

    /** Integer footprint scale: 2x1, 2x2, 1x2, centered in the cell box. */
    private void renderScaledItem(GuiGraphics graphics, ItemStack stack, int x, int y, int boxW, int boxH, int scaleX, int scaleY) {
        int iconW = ICON * scaleX;
        int iconH = ICON * scaleY;
        int ox = x + Math.max(0, (boxW - iconW) / 2);
        int oy = y + Math.max(0, (boxH - iconH) / 2);
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(ox, oy, 0);
        pose.scale(scaleX, scaleY, 1.0F);
        graphics.renderItem(stack, 0, 0);
        pose.popPose();
        graphics.renderItemDecorations(font, stack, ox + iconW - ICON, oy + iconH - ICON);
    }

    private void renderWeaponRail(GuiGraphics graphics, Slot slot) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(slot.x + InventoryLayout.WEAPON_WIDTH / 2.0F, slot.y + InventoryLayout.ITEM / 2.0F, 150);
        pose.mulPose(new Quaternionf().rotateZ(Mth.DEG_TO_RAD * 90));
        pose.translate(-8, -8, 0);
        graphics.renderItem(stack, 0, 0);
        pose.popPose();
        graphics.renderItemDecorations(font, stack, slot.x + InventoryLayout.WEAPON_WIDTH - ICON, slot.y + (InventoryLayout.ITEM - ICON) / 2);
    }

    private void drawFramed(GuiGraphics g, int x, int y, int w, int h, ItemStack stack, boolean hover) {
        int border = InventoryLayout.LINE;
        int fill = InventoryLayout.SLOT;
        if (!stack.isEmpty()) {
            var quality = ExModifierApi.qualityOn(stack);
            border = QualityChrome.accent(stack.getRarity(), quality);
            fill = RarityPalette.theme(stack.getRarity(), quality).slotFill();
        }
        if (hover) {
            border = RarityPalette.brighten(border, 0.28f);
            fill = RarityPalette.brighten(fill, 0.10f);
        }
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        g.fill(x, y, x + w, y + h, fill);
    }

    private void drawFill(GuiGraphics g, int x, int y, int w, int h, ItemStack stack, boolean locked, boolean hover) {
        int fill = InventoryLayout.SLOT;
        int edge = 0;
        if (!stack.isEmpty()) {
            var quality = ExModifierApi.qualityOn(stack);
            edge = QualityChrome.accent(stack.getRarity(), quality);
            fill = RarityPalette.theme(stack.getRarity(), quality).slotFill();
        }
        if (locked) {
            fill = InventoryLayout.SLOT_LOCK;
            edge = 0;
        } else if (hover) {
            fill = RarityPalette.brighten(fill, 0.12f);
            if (edge == 0) edge = InventoryLayout.ACCENT;
        }
        g.fill(x, y, x + w, y + h, fill);
        if (edge != 0) {
            g.fill(x, y, x + w, y + 1, edge);
            g.fill(x, y + h - 1, x + w, y + h, edge);
            g.fill(x, y, x + 1, y + h, edge);
            g.fill(x + w - 1, y, x + w, y + h, edge);
        }
    }

    private static void fillAlpha(GuiGraphics g, int x1, int y1, int x2, int y2, int color, int alpha) {
        g.fill(x1, y1, x2, y2, (color & 0x00FFFFFF) | (alpha << 24));
    }

    private void drawTab(GuiGraphics graphics, int x, int y, Component label, boolean active) {
        drawTab(graphics, x, y, label, active, false);
    }

    private void drawTab(GuiGraphics graphics, int x, int y, Component label, boolean active, boolean hovered) {
        int bg = active ? InventoryLayout.SURFACE : hovered ? InventoryLayout.BUTTON_HOVER : InventoryLayout.TAB_IDLE;
        graphics.fill(x, y, x + 60, y + InventoryLayout.TAB_H, bg);
        if (active) {
            graphics.fill(x, y + InventoryLayout.TAB_H - 1, x + 60, y + InventoryLayout.TAB_H, InventoryLayout.ACCENT);
        } else {
            graphics.fill(x, y + InventoryLayout.TAB_H - 1, x + 60, y + InventoryLayout.TAB_H, InventoryLayout.LINE_INNER);
        }
        graphics.drawCenteredString(font, label, x + 30, y + 7, active ? InventoryLayout.TEXT : InventoryLayout.MUTED);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int w, int h, Component label, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, hover ? InventoryLayout.ACCENT : InventoryLayout.LINE);
        graphics.fill(x, y, x + w, y + h, hover ? InventoryLayout.BUTTON_HOVER : InventoryLayout.BUTTON);
        graphics.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, InventoryLayout.TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int bx = leftPos + InventoryLayout.ACCESSORY_BTN_X;
        int by = topPos + InventoryLayout.ACCESSORY_BTN_Y;
        boolean inPanel = mouseX >= leftPos && mouseX < leftPos + imageWidth && mouseY >= topPos && mouseY < topPos + imageHeight;
        if (inPanel) playClick();
        if (mouseX >= bx && mouseX < bx + InventoryLayout.ACCESSORY_BTN_W
                && mouseY >= by && mouseY < by + InventoryLayout.ACCESSORY_BTN_H) {
            BackpackUi.accessories = !BackpackUi.accessories;
            return true;
        }
        int tabX = leftPos + 192;
        int tabY = topPos;
        if (BackpackTabs.hasSequence()
                && mouseX >= tabX && mouseX < tabX + 60 && mouseY >= tabY && mouseY < tabY + InventoryLayout.TAB_H) {
            BackpackTabs.openSequence();
            return true;
        }
        if (minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2)) {
            int ex = leftPos + InventoryLayout.EDIT_BTN_X;
            int ey = topPos + InventoryLayout.EDIT_BTN_Y;
            if (mouseX >= ex && mouseX < ex + InventoryLayout.ACCESSORY_BTN_W
                    && mouseY >= ey && mouseY < ey + InventoryLayout.ACCESSORY_BTN_H) {
                PacketDistributor.sendToServer(new InventoryPayloads.RequestFootprintEditorPayload());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClick() {
        if (minecraft != null && minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_R) {
            PacketDistributor.sendToServer(new InventoryPayloads.RotateBackpackPayload(hoveredIndex()));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hoveredSlot != null && hoveredSlot.index < PlayerBackpackMenu.GRID_SLOTS && scrollY != 0) {
            PacketDistributor.sendToServer(new InventoryPayloads.RotateBackpackPayload(hoveredIndex()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        BackpackUi.accessories = false;
        super.onClose();
    }

    private int hoveredIndex() {
        return hoveredSlot == null ? -1 : hoveredSlot.index;
    }
}
