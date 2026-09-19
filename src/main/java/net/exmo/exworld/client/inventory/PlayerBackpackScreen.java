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
        renderTooltip(graphics, mouseX, mouseY);
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
        fillAlpha(graphics, split, y + InventoryLayout.TAB_H + 8, split + 1, y + imageHeight - 8, InventoryLayout.LINE, fadeA);

        drawTab(graphics, x, y, Component.translatable("screen.exworld.backpack_tab"), true);
        drawTab(graphics, x + 64, y, Component.translatable("screen.exworld.character_tab"), false);
        drawTab(graphics, x + 128, y, Component.translatable("screen.exworld.quests_tab"), false);

        boolean accessories = BackpackUi.accessories;
        drawButton(graphics,
                x + InventoryLayout.ACCESSORY_BTN_X,
                y + InventoryLayout.ACCESSORY_BTN_Y,
                InventoryLayout.ACCESSORY_BTN_W,
                InventoryLayout.ACCESSORY_BTN_H,
                Component.translatable(accessories ? "screen.exworld.backpack_equipment" : "screen.exworld.backpack_accessories"),
                mouseX, mouseY);
        if (minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2)) {
            drawButton(graphics,
                    x + InventoryLayout.EDIT_BTN_X,
                    y + InventoryLayout.EDIT_BTN_Y,
                    InventoryLayout.ACCESSORY_BTN_W,
                    InventoryLayout.ACCESSORY_BTN_H,
                    Component.translatable("screen.exworld.footprint_edit"),
                    mouseX, mouseY);
        }

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
            drawCell(graphics, x + InventoryLayout.WEAPON1_X, y + InventoryLayout.WEAPON1_Y,
                    InventoryLayout.WEAPON_WIDTH, InventoryLayout.ITEM, menu.backpack().weapon(0),
                    false, hoverRect(mouseX, mouseY, x + InventoryLayout.WEAPON1_X, y + InventoryLayout.WEAPON1_Y, InventoryLayout.WEAPON_WIDTH, InventoryLayout.ITEM), now);
            drawCell(graphics, x + InventoryLayout.WEAPON2_X, y + InventoryLayout.WEAPON2_Y,
                    InventoryLayout.WEAPON_WIDTH, InventoryLayout.ITEM, menu.backpack().weapon(1),
                    false, hoverRect(mouseX, mouseY, x + InventoryLayout.WEAPON2_X, y + InventoryLayout.WEAPON2_Y, InventoryLayout.WEAPON_WIDTH, InventoryLayout.ITEM), now);
            drawCell(graphics, x + InventoryLayout.HELMET_X, y + InventoryLayout.HELMET_Y,
                    InventoryLayout.ITEM, InventoryLayout.ITEM, menu.slots.get(PlayerBackpackMenu.ARMOR_START).getItem(), false,
                    hoverRect(mouseX, mouseY, x + InventoryLayout.HELMET_X, y + InventoryLayout.HELMET_Y, InventoryLayout.ITEM, InventoryLayout.ITEM), now);
            drawCell(graphics, x + InventoryLayout.CHEST_X, y + InventoryLayout.CHEST_Y,
                    InventoryLayout.ITEM, InventoryLayout.ITEM, menu.slots.get(PlayerBackpackMenu.ARMOR_START + 1).getItem(), false,
                    hoverRect(mouseX, mouseY, x + InventoryLayout.CHEST_X, y + InventoryLayout.CHEST_Y, InventoryLayout.ITEM, InventoryLayout.ITEM), now);
            drawCell(graphics, x + InventoryLayout.LEGS_X, y + InventoryLayout.LEGS_Y,
                    InventoryLayout.ITEM, InventoryLayout.ITEM, menu.slots.get(PlayerBackpackMenu.ARMOR_START + 2).getItem(), false,
                    hoverRect(mouseX, mouseY, x + InventoryLayout.LEGS_X, y + InventoryLayout.LEGS_Y, InventoryLayout.ITEM, InventoryLayout.ITEM), now);
            drawCell(graphics, x + InventoryLayout.BOOTS_X, y + InventoryLayout.BOOTS_Y,
                    InventoryLayout.ITEM, InventoryLayout.ITEM, menu.slots.get(PlayerBackpackMenu.ARMOR_START + 3).getItem(), false,
                    hoverRect(mouseX, mouseY, x + InventoryLayout.BOOTS_X, y + InventoryLayout.BOOTS_Y, InventoryLayout.ITEM, InventoryLayout.ITEM), now);
            drawCell(graphics, x + InventoryLayout.CORE_X, y + InventoryLayout.CORE_Y,
                    InventoryLayout.ITEM, InventoryLayout.ITEM, menu.backpack().data().core(), false,
                    hoverRect(mouseX, mouseY, x + InventoryLayout.CORE_X, y + InventoryLayout.CORE_Y, InventoryLayout.ITEM, InventoryLayout.ITEM), now);
        } else {
            for (int i = 0; i < 6; i++) {
                int ax = x + InventoryLayout.ACCESSORY_X + (i % 3) * InventoryLayout.CELL;
                int ay = y + InventoryLayout.ACCESSORY_Y + (i / 3) * InventoryLayout.CELL;
                drawCell(graphics, ax, ay, InventoryLayout.ITEM, InventoryLayout.ITEM,
                        menu.backpack().data().accessory(i), false,
                        hoverRect(mouseX, mouseY, ax, ay, InventoryLayout.ITEM, InventoryLayout.ITEM), now);
            }
        }

        for (int cell = 0; cell < StorageCore.GRID_CELLS; cell++) {
            int owner = menu.backpack().ownerOf(cell);
            if (owner >= 0 && owner != cell) continue;
            ItemStack stack = menu.backpack().grid(cell);
            ItemFootprint footprint = stack.isEmpty() ? ItemFootprint.UNIT : ItemStackOps.INSTANCE.footprint(stack);
            int gx = x + gridX(cell);
            int gy = y + gridY(cell);
            drawCell(graphics, gx, gy,
                    footprint.width() * InventoryLayout.CELL - InventoryLayout.GAP,
                    footprint.height() * InventoryLayout.CELL - InventoryLayout.GAP,
                    stack,
                    cell >= unlocked,
                    cell == hovered,
                    now);
        }
        for (int i = 0; i < 9; i++) {
            int hx = x + InventoryLayout.GRID_X + i * InventoryLayout.CELL;
            drawCell(graphics, hx, y + InventoryLayout.HOTBAR_Y, InventoryLayout.ITEM, InventoryLayout.ITEM,
                    menu.slots.get(PlayerBackpackMenu.HOTBAR_START + i).getItem(), false,
                    hoverRect(mouseX, mouseY, hx, y + InventoryLayout.HOTBAR_Y, InventoryLayout.ITEM, InventoryLayout.ITEM), now);
        }
    }

    private int hoveredGridCell(int mouseX, int mouseY) {
        int gx = mouseX - leftPos - InventoryLayout.GRID_X;
        int gy = mouseY - topPos - InventoryLayout.GRID_Y;
        int col = gx / InventoryLayout.CELL;
        int row = gy / InventoryLayout.CELL;
        if (col >= 0 && col < InventoryLayout.COLUMNS && row >= 0 && row < InventoryLayout.ROWS) {
            return row * InventoryLayout.COLUMNS + col;
        }
        return -1;
    }

    private static boolean hoverRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int gridX(int cell) {
        return InventoryLayout.GRID_X + (cell % InventoryLayout.COLUMNS) * InventoryLayout.CELL;
    }

    private static int gridY(int cell) {
        return InventoryLayout.GRID_Y + (cell / InventoryLayout.COLUMNS) * InventoryLayout.CELL;
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
        super.renderSlot(graphics, slot);
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (slot.index >= PlayerBackpackMenu.GRID_SLOTS) super.renderSlotHighlight(graphics, slot, mouseX, mouseY, partialTick);
        // Grid highlight is painted as part of the custom cell frame.
    }

    private void renderGridItem(GuiGraphics graphics, Slot slot) {
        int owner = menu.backpack().ownerOf(slot.index);
        if (owner >= 0 && owner != slot.index) return;
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return;
        ItemFootprint footprint = ItemStackOps.INSTANCE.footprint(stack);
        var pose = graphics.pose();
        pose.pushPose();
        if (footprint.width() > 1 || footprint.height() > 1) {
            float sx = (footprint.width() * InventoryLayout.CELL - InventoryLayout.GAP) / 16.0F;
            float sy = (footprint.height() * InventoryLayout.CELL - InventoryLayout.GAP) / 16.0F;
            pose.translate(slot.x, slot.y, 0);
            pose.scale(sx, sy, 1.0F);
            graphics.renderItem(stack, 0, 0);
            graphics.renderItemDecorations(font, stack, 0, 0);
        } else {
            graphics.renderItem(stack, slot.x, slot.y);
            graphics.renderItemDecorations(font, stack, slot.x, slot.y);
        }
        pose.popPose();
    }

    private void renderWeaponRail(GuiGraphics graphics, Slot slot) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(slot.x + InventoryLayout.WEAPON_WIDTH / 2.0F, slot.y + 8, 150);
        pose.mulPose(new Quaternionf().rotateZ(Mth.DEG_TO_RAD * 90));
        pose.translate(-8, -8, 0);
        graphics.renderItem(stack, 0, 0);
        pose.popPose();
        graphics.renderItemDecorations(font, stack, slot.x + InventoryLayout.WEAPON_WIDTH - InventoryLayout.ITEM, slot.y);
    }

    /** Modern slot chrome: soft gradient, rarity border, corner ticks and a breathing glow. */
    private void drawCell(GuiGraphics g, int x, int y, int w, int h, ItemStack stack, boolean locked, boolean hover, long now) {
        int border = InventoryLayout.LINE;
        int fillTop = InventoryLayout.SLOT;
        int fillBottom = RarityPalette.darken(InventoryLayout.SLOT, 0.35f);
        boolean rarity = false;

        if (!stack.isEmpty()) {
            var quality = ExModifierApi.qualityOn(stack);
            int accent = QualityChrome.accent(stack.getRarity(), quality);
            var theme = RarityPalette.theme(stack.getRarity(), quality);
            border = accent;
            fillTop = theme.slotFill();
            fillBottom = (RarityPalette.darken(accent, 0.86f) & 0x00FFFFFF) | 0xFF000000;
            rarity = true;
        }
        if (locked) {
            border = InventoryLayout.LINE;
            fillTop = InventoryLayout.SLOT_LOCK;
            fillBottom = InventoryLayout.SLOT_LOCK;
            rarity = false;
        }
        if (hover && !locked) {
            border = RarityPalette.brighten(border, 0.35f);
            fillTop = RarityPalette.brighten(fillTop, 0.10f);
            fillBottom = RarityPalette.brighten(fillBottom, 0.10f);
        }

        if (rarity) {
            float pulse = 0.5F + 0.5F * (float) Math.sin(now * 0.006);
            int glowAlpha = (int) (40 + 48 * pulse);
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, (border & 0x00FFFFFF) | (glowAlpha << 24));
        } else if (hover) {
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x38FFFFFF);
        }

        fillGradient(g, x, y, w, h, fillTop, fillBottom);

        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);

        if (rarity || hover) {
            int tick = RarityPalette.brighten(border, 0.55f);
            int t = 2;
            g.fill(x, y, x + t, y + 1, tick);
            g.fill(x, y, x + 1, y + t, tick);
            g.fill(x + w - t, y, x + w, y + 1, tick);
            g.fill(x + w - 1, y, x + w, y + t, tick);
            g.fill(x, y + h - t, x + t, y + h, tick);
            g.fill(x, y + h - 1, x + 1, y + h, tick);
            g.fill(x + w - t, y + h - 1, x + w, y + h, tick);
            g.fill(x + w - 1, y + h - t, x + w, y + h, tick);
        }
    }

    private static void fillGradient(GuiGraphics g, int x, int y, int w, int h, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        for (int i = 0; i < h; i++) {
            float t = h == 1 ? 0F : i / (float) (h - 1);
            g.fill(x, y + i, x + w, y + i + 1, RarityPalette.lerp(top, bottom, t));
        }
    }

    private static void fillAlpha(GuiGraphics g, int x1, int y1, int x2, int y2, int color, int alpha) {
        g.fill(x1, y1, x2, y2, (color & 0x00FFFFFF) | (alpha << 24));
    }

    private void drawTab(GuiGraphics graphics, int x, int y, Component label, boolean active) {
        long now = System.currentTimeMillis();
        if (active) {
            graphics.fill(x, y, x + 60, y + InventoryLayout.TAB_H, InventoryLayout.SURFACE);
            int pulse = 160 + (int) (80 * (0.5 + 0.5 * Math.sin(now * 0.004)));
            graphics.fill(x, y + InventoryLayout.TAB_H - 1, x + 60, y + InventoryLayout.TAB_H,
                    (InventoryLayout.ACCENT & 0x00FFFFFF) | (Math.min(255, pulse) << 24));
        } else {
            graphics.fill(x, y, x + 60, y + InventoryLayout.TAB_H, 0xFF0C0C0C);
        }
        graphics.drawCenteredString(font, label, x + 30, y + 7, active ? InventoryLayout.TEXT : InventoryLayout.MUTED);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int w, int h, Component label, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int bg = hover ? 0xFF2E2E2E : 0xFF181818;
        fillGradient(graphics, x, y, w, h, bg, RarityPalette.darken(bg, 0.25f));
        graphics.fill(x, y, x + w, y + 1, hover ? InventoryLayout.ACCENT : InventoryLayout.LINE);
        graphics.fill(x, y + h - 1, x + w, y + h, InventoryLayout.LINE);
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
