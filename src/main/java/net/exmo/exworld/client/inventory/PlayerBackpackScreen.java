package net.exmo.exworld.client.inventory;

import net.exmo.exmodifier.api.ExModifierApi;
import net.exmo.exworld.client.tooltip.QualityChrome;
import net.exmo.exworld.client.tooltip.RarityPalette;
import net.exmo.exworld.inventory.BackpackUi;
import net.exmo.exworld.inventory.CuriosPresence;
import net.exmo.exworld.inventory.InventoryLayout;
import net.exmo.exworld.inventory.ItemFootprint;
import net.exmo.exworld.inventory.ItemStackOps;
import net.exmo.exworld.inventory.PlayerBackpackData;
import net.exmo.exworld.inventory.PlayerBackpackMenu;
import net.exmo.exworld.inventory.StorageCore;
import net.exmo.exworld.network.InventoryPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class PlayerBackpackScreen extends AbstractContainerScreen<PlayerBackpackMenu> {
    private static final int ICON = 16;

    private float xMouse;
    private float yMouse;
    private int accessoryScroll;
    private int accessoryMax;
    private int viewX;
    private int viewY;
    private int viewW;
    private int viewH;
    private boolean draggingBar;
    /** Left click already sent a slot action; vanilla would click again on release. */
    private boolean swallowRelease;
    private final List<CurioHit> curioHits = new ArrayList<>();
    private final List<LegacyHit> legacyHits = new ArrayList<>();

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
        prepareAccessoryLayout();
        super.render(graphics, mouseX, mouseY, partialTick);
        drawControlLayer(graphics, mouseX, mouseY);
        renderHoverTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xC008080C);
        renderBg(graphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        float fade = InventoryChrome.fade(BackpackUi.openTime);
        InventoryChrome.panel(graphics, x, y + InventoryLayout.TAB_H, imageWidth, imageHeight - InventoryLayout.TAB_H, fade);

        drawTab(graphics, x, y, Component.translatable("screen.exworld.backpack_tab"), true, false);
        int sequenceX = BackpackTabs.sequenceTabX(x);
        if (sequenceX >= 0) {
            boolean sequenceHover = hoverRect(mouseX, mouseY, sequenceX, y, BackpackTabs.TAB_W, InventoryLayout.TAB_H);
            drawTab(graphics, sequenceX, y, Component.translatable("screen.exworld.sequence_tab"), false, sequenceHover);
        }
        int characterX = BackpackTabs.characterTabX(x);
        boolean characterHover = hoverRect(mouseX, mouseY, characterX, y, BackpackTabs.TAB_W, InventoryLayout.TAB_H);
        drawTab(graphics, characterX, y, Component.translatable("screen.exworld.character_tab"), false, characterHover);

        boolean accessories = BackpackUi.accessories;
        int hovered = hoveredGridCell(mouseX, mouseY);
        int unlocked = menu.backpack().unlocked();

        if (!accessories) {
            graphics.fill(x + InventoryLayout.DOLL_X1, y + InventoryLayout.DOLL_Y1,
                    x + InventoryLayout.DOLL_X2, y + InventoryLayout.DOLL_Y2, InventoryLayout.SURFACE_INNER);
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
        } else if (!CuriosPresence.loaded()) {
            drawPlate(graphics, x + InventoryLayout.ACCESSORY_X, y + InventoryLayout.ACCESSORY_Y, 3, 2);
            for (int i = 0; i < PlayerBackpackData.ACCESSORY_COUNT; i++) {
                int ax = x + InventoryLayout.ACCESSORY_X + (i % 3) * InventoryLayout.CELL;
                int ay = y + InventoryLayout.ACCESSORY_Y + (i / 3) * InventoryLayout.CELL;
                drawFill(graphics, ax, ay, InventoryLayout.ITEM, InventoryLayout.ITEM,
                        menu.backpack().data().accessory(i), false,
                        hoverRect(mouseX, mouseY, ax, ay, InventoryLayout.ITEM, InventoryLayout.ITEM));
            }
        } else {
            drawCurios(graphics, mouseX, mouseY);
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

    private void drawCurios(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(viewX, viewY, viewX + viewW, viewY + viewH, InventoryLayout.SURFACE_INNER);
        graphics.enableScissor(viewX, viewY, viewX + viewW - 4, viewY + viewH);
        int y = viewY - accessoryScroll;
        if (minecraft != null && minecraft.player != null) {
            List<CuriosPresence.GroupView> groups = CuriosPresence.groups(minecraft.player);
            if (groups.isEmpty() && !hasLegacyAccessories()) {
                graphics.drawString(font, Component.translatable("screen.exworld.curios_empty"), viewX + 6, viewY + 6, InventoryLayout.MUTED, false);
            }
            int columns = Math.max(1, (viewW - 8) / InventoryLayout.CELL);
            for (CuriosPresence.GroupView group : groups) {
                graphics.drawString(font, fit(slotLabel(group.identifier()), viewW - 12), viewX + 4, y + 2, InventoryLayout.MUTED, false);
                y += 12;
                for (int i = 0; i < group.slots().size(); i++) {
                    CuriosPresence.SlotView slot = group.slots().get(i);
                    int sx = viewX + 4 + (i % columns) * InventoryLayout.CELL;
                    int sy = y + (i / columns) * InventoryLayout.CELL;
                    boolean hover = hoverRect(mouseX, mouseY, sx, sy, InventoryLayout.ITEM, InventoryLayout.ITEM)
                            && mouseY >= viewY && mouseY < viewY + viewH;
                    drawFill(graphics, sx, sy, InventoryLayout.ITEM, InventoryLayout.ITEM, slot.stack(), false, hover);
                    drawAccessoryItem(graphics, slot.stack(), sx, sy);
                    if (slot.stack().isEmpty()) drawCurioIcon(graphics, slot.icon(), sx + 2, sy + 2);
                }
                int rows = (group.slots().size() + columns - 1) / columns;
                y += rows * InventoryLayout.CELL + 4;
            }
            if (hasLegacyAccessories()) {
                graphics.drawString(font, Component.translatable("screen.exworld.legacy_accessories"), viewX + 4, y + 2, InventoryLayout.MUTED, false);
                y += 12;
                for (int i = 0; i < PlayerBackpackData.ACCESSORY_COUNT; i++) {
                    int sx = viewX + 4 + (i % 3) * InventoryLayout.CELL;
                    int sy = y + (i / 3) * InventoryLayout.CELL;
                    drawFill(graphics, sx, sy, InventoryLayout.ITEM, InventoryLayout.ITEM,
                            menu.backpack().data().accessory(i), false,
                            hoverRect(mouseX, mouseY, sx, sy, InventoryLayout.ITEM, InventoryLayout.ITEM));
                    drawAccessoryItem(graphics, menu.backpack().data().accessory(i), sx, sy);
                }
            }
        }
        graphics.disableScissor();
        InventoryChrome.scrollbar(graphics, viewX + viewW - 3, viewY, viewH, accessoryMax, accessoryScroll);
    }

    private void prepareAccessoryLayout() {
        curioHits.clear();
        viewX = leftPos + InventoryLayout.PAD;
        viewY = topPos + InventoryLayout.TAB_H + 28;
        viewW = InventoryLayout.LEFT_WIDTH - 8;
        viewH = Math.max(20, topPos + imageHeight - 8 - viewY);
        legacyHits.clear();
        if (!BackpackUi.accessories || !CuriosPresence.loaded() || minecraft == null || minecraft.player == null) {
            accessoryMax = 0;
            return;
        }
        int columns = Math.max(1, (viewW - 8) / InventoryLayout.CELL);
        int content = 0;
        List<CuriosPresence.GroupView> groups = CuriosPresence.groups(minecraft.player);
        for (CuriosPresence.GroupView group : groups) {
            content += 12;
            int rows = (group.slots().size() + columns - 1) / columns;
            content += rows * InventoryLayout.CELL + 4;
        }
        if (hasLegacyAccessories()) content += 12 + 2 * InventoryLayout.CELL;
        accessoryMax = Math.max(0, content - viewH);
        accessoryScroll = Mth.clamp(accessoryScroll, 0, accessoryMax);

        int y = viewY - accessoryScroll;
        for (CuriosPresence.GroupView group : groups) {
            y += 12;
            for (int i = 0; i < group.slots().size(); i++) {
                CuriosPresence.SlotView slot = group.slots().get(i);
                int sx = viewX + 4 + (i % columns) * InventoryLayout.CELL;
                int sy = y + (i / columns) * InventoryLayout.CELL;
                if (sy + InventoryLayout.ITEM > viewY && sy < viewY + viewH) {
                    curioHits.add(new CurioHit(slot.identifier(), slot.index(), sx, sy));
                }
            }
            int rows = (group.slots().size() + columns - 1) / columns;
            y += rows * InventoryLayout.CELL + 4;
        }
        if (hasLegacyAccessories()) {
            y += 12;
            for (int i = 0; i < PlayerBackpackData.ACCESSORY_COUNT; i++) {
                int sx = viewX + 4 + (i % 3) * InventoryLayout.CELL;
                int sy = y + (i / 3) * InventoryLayout.CELL;
                if (sy + InventoryLayout.ITEM > viewY && sy < viewY + viewH) {
                    legacyHits.add(new LegacyHit(i, sx, sy));
                }
            }
        }
    }

    private boolean hasLegacyAccessories() {
        for (int i = 0; i < PlayerBackpackData.ACCESSORY_COUNT; i++) {
            if (!menu.backpack().data().accessory(i).isEmpty()) return true;
        }
        return false;
    }

    private Component slotLabel(String identifier) {
        String key = "curios.identifier." + identifier;
        Component translated = Component.translatable(key);
        return translated.getString().equals(key) ? Component.literal(identifier) : translated;
    }

    private String fit(Component component, int max) {
        String text = component.getString();
        if (font.width(text) <= max) return text;
        String ellipsis = "…";
        int limit = Math.max(0, max - font.width(ellipsis));
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end)) > limit) end--;
        return text.substring(0, end) + ellipsis;
    }

    private void drawCurioIcon(GuiGraphics graphics, ResourceLocation icon, int x, int y) {
        if (minecraft == null || icon == null) return;
        TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(icon);
        graphics.blit(x, y, 0, 16, 16, sprite);
    }

    /**
     * Curios slots are projected into this screen instead of being menu slots, so vanilla never
     * reaches {@link #renderSlot(GuiGraphics, Slot)} for them. Render their stack immediately
     * after the custom slot fill; otherwise the fill is the only visible layer.
     */
    private void drawAccessoryItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        int pad = (InventoryLayout.ITEM - ICON) / 2;
        int itemX = x + pad;
        int itemY = y + pad;
        graphics.renderItem(stack, itemX, itemY);
        graphics.renderItemDecorations(font, stack, itemX, itemY);
    }

    private static void drawPlate(GuiGraphics graphics, int x, int y, int columns, int rows) {
        int w = columns * InventoryLayout.CELL - InventoryLayout.GAP;
        int h = rows * InventoryLayout.CELL - InventoryLayout.GAP;
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, InventoryLayout.LINE);
        graphics.fill(x, y, x + w, y + h, InventoryLayout.LINE_INNER);
    }

    private static void drawUnlockedPlate(GuiGraphics graphics, int x, int y, int unlocked) {
        int shown = Math.max(0, Math.min(StorageCore.GRID_CELLS, unlocked));
        if (shown <= 0) return;
        int fullRows = shown / InventoryLayout.COLUMNS;
        int extra = shown % InventoryLayout.COLUMNS;
        if (fullRows > 0) drawPlate(graphics, x, y, InventoryLayout.COLUMNS, fullRows);
        if (extra > 0) drawPlate(graphics, x, y + fullRows * InventoryLayout.CELL, extra, 1);
    }

    private void drawControlLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 500);
        int bx = leftPos + InventoryLayout.ACCESSORY_BTN_X;
        int by = topPos + InventoryLayout.ACCESSORY_BTN_Y;
        int size = InventoryLayout.ACCESSORY_BTN;
        boolean hover = hoverRect(mouseX, mouseY, bx, by, size, size);
        graphics.fill(bx - 1, by - 1, bx + size + 1, by + size + 1, hover ? InventoryLayout.ACCENT : InventoryLayout.LINE);
        graphics.fill(bx, by, bx + size, by + size, hover ? InventoryLayout.BUTTON_HOVER : InventoryLayout.BUTTON);
        int mark = hover ? InventoryLayout.TEXT : InventoryLayout.MUTED;
        graphics.fill(bx + 5, by + 5, bx + size - 5, by + 6, mark);
        graphics.fill(bx + 5, by + size - 6, bx + size - 5, by + size - 5, mark);
        graphics.fill(bx + 5, by + 5, bx + 6, by + size - 5, mark);
        graphics.fill(bx + size - 6, by + 5, bx + size - 5, by + size - 5, mark);
        graphics.fill(bx + 8, by + 8, bx + size - 8, by + size - 8, mark);
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
        graphics.fill(gx, gy, gx + w, gy + h, origin >= 0 ? 0x55E4EDF6 : 0x66C45C5C);
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

    private boolean pickup(int slot, int button) {
        if (minecraft == null || minecraft.gameMode == null || minecraft.player == null || slot < 0) return false;
        net.minecraft.world.inventory.ClickType type = hasShiftDown()
                ? net.minecraft.world.inventory.ClickType.QUICK_MOVE
                : net.minecraft.world.inventory.ClickType.PICKUP;
        minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slot, button, type, minecraft.player);
        return true;
    }


    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private void renderHoverTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!menu.getCarried().isEmpty()) return;
        if (hoveringAccessoryButton(mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(BackpackUi.accessories
                    ? "screen.exworld.backpack_equipment" : "screen.exworld.backpack_accessories"), mouseX, mouseY);
            return;
        }
        ItemStack stack = stackAt(mouseX, mouseY);
        if (stack.isEmpty()) return;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 800);
        graphics.renderTooltip(font, stack, mouseX, mouseY);
        pose.popPose();
    }

    private ItemStack stackAt(int mouseX, int mouseY) {
        int cell = hoveredGridCell(mouseX, mouseY);
        if (cell >= 0) {
            int owner = menu.backpack().ownerOf(cell);
            return owner >= 0 ? menu.backpack().grid(owner) : ItemStack.EMPTY;
        }
        for (int i = 0; i < 9; i++) {
            int hx = leftPos + InventoryLayout.GRID_X + i * InventoryLayout.CELL;
            if (hoverRect(mouseX, mouseY, hx, topPos + InventoryLayout.HOTBAR_Y, InventoryLayout.CELL, InventoryLayout.CELL)) {
                return menu.slots.get(PlayerBackpackMenu.HOTBAR_START + i).getItem();
            }
        }
        if (!BackpackUi.accessories) {
            if (!net.exmo.exworld.Config.decryptionMode) {
                if (hoverRect(mouseX, mouseY, leftPos + InventoryLayout.WEAPON1_X, topPos + InventoryLayout.WEAPON1_Y, InventoryLayout.WEAPON_WIDTH, InventoryLayout.ITEM)) {
                    return menu.backpack().weapon(0);
                }
                if (hoverRect(mouseX, mouseY, leftPos + InventoryLayout.WEAPON2_X, topPos + InventoryLayout.WEAPON2_Y, InventoryLayout.WEAPON_WIDTH, InventoryLayout.ITEM)) {
                    return menu.backpack().weapon(1);
                }
            }
            ItemStack armor = armorAt(mouseX, mouseY);
            if (!armor.isEmpty()) return armor;
            if (hoverRect(mouseX, mouseY, leftPos + InventoryLayout.CORE_X, topPos + InventoryLayout.CORE_Y, InventoryLayout.ITEM, InventoryLayout.ITEM)) {
                return menu.backpack().data().core();
            }
            return ItemStack.EMPTY;
        }
        for (CurioHit hit : curioHits) {
            if (hoverRect(mouseX, mouseY, hit.x, hit.y, InventoryLayout.ITEM, InventoryLayout.ITEM)) {
                return curioStack(hit.identifier, hit.index);
            }
        }
        for (LegacyHit hit : legacyHits) {
            if (hoverRect(mouseX, mouseY, hit.x, hit.y, InventoryLayout.ITEM, InventoryLayout.ITEM)) {
                return menu.backpack().data().accessory(hit.index);
            }
        }
        for (int i = 0; i < PlayerBackpackData.ACCESSORY_COUNT; i++) {
            int ax = leftPos + InventoryLayout.ACCESSORY_X + (i % 3) * InventoryLayout.CELL;
            int ay = topPos + InventoryLayout.ACCESSORY_Y + (i / 3) * InventoryLayout.CELL;
            if (hoverRect(mouseX, mouseY, ax, ay, InventoryLayout.ITEM, InventoryLayout.ITEM)) {
                return menu.backpack().data().accessory(i);
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack armorAt(int mouseX, int mouseY) {
        int[] xs = {InventoryLayout.HELMET_X, InventoryLayout.CHEST_X, InventoryLayout.LEGS_X, InventoryLayout.BOOTS_X};
        int[] ys = {InventoryLayout.HELMET_Y, InventoryLayout.CHEST_Y, InventoryLayout.LEGS_Y, InventoryLayout.BOOTS_Y};
        for (int i = 0; i < 4; i++) {
            if (hoverRect(mouseX, mouseY, leftPos + xs[i], topPos + ys[i], InventoryLayout.ITEM, InventoryLayout.ITEM)) {
                return menu.slots.get(PlayerBackpackMenu.ARMOR_START + i).getItem();
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack curioStack(String identifier, int index) {
        if (minecraft == null || minecraft.player == null) return ItemStack.EMPTY;
        for (CuriosPresence.GroupView group : CuriosPresence.groups(minecraft.player)) {
            if (!group.identifier().equals(identifier)) continue;
            for (CuriosPresence.SlotView slot : group.slots()) {
                if (slot.index() == index) return slot.stack();
            }
        }
        return ItemStack.EMPTY;
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

    private void drawTab(GuiGraphics graphics, int x, int y, Component label, boolean active, boolean hovered) {
        int bg = active ? InventoryChrome.theme().titleBar() : hovered ? InventoryLayout.BUTTON_HOVER : InventoryLayout.TAB_IDLE;
        graphics.fill(x, y, x + 60, y + InventoryLayout.TAB_H, bg);
        graphics.fill(x, y + InventoryLayout.TAB_H - 1, x + 60, y + InventoryLayout.TAB_H,
                active ? InventoryChrome.theme().border() : InventoryLayout.LINE_INNER);
        graphics.drawCenteredString(font, label, x + 30, y + 7, active ? InventoryLayout.TEXT : InventoryLayout.MUTED);
    }

    private boolean hoveringAccessoryButton(int mouseX, int mouseY) {
        return hoverRect(mouseX, mouseY,
                leftPos + InventoryLayout.ACCESSORY_BTN_X,
                topPos + InventoryLayout.ACCESSORY_BTN_Y,
                InventoryLayout.ACCESSORY_BTN,
                InventoryLayout.ACCESSORY_BTN);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        swallowRelease = true;
        if (hoveringAccessoryButton((int) mouseX, (int) mouseY)) {
            BackpackUi.accessories = !BackpackUi.accessories;
            accessoryScroll = 0;
            return true;
        }
        int sequenceX = BackpackTabs.sequenceTabX(leftPos);
        if (sequenceX >= 0
                && hoverRect((int) mouseX, (int) mouseY, sequenceX, topPos, BackpackTabs.TAB_W, InventoryLayout.TAB_H)) {
            BackpackTabs.openSequence();
            return true;
        }
        int characterX = BackpackTabs.characterTabX(leftPos);
        if (hoverRect((int) mouseX, (int) mouseY, characterX, topPos, BackpackTabs.TAB_W, InventoryLayout.TAB_H)) {
            PacketDistributor.sendToServer(new net.exmo.exworld.network.CharacterPayloads.OpenCharacterPayload());
            return true;
        }
        if (BackpackUi.accessories && CuriosPresence.loaded() && accessoryMax > 0
                && hoverRect((int) mouseX, (int) mouseY, viewX + viewW - 4, viewY, 4, viewH)) {
            draggingBar = true;
            return true;
        }
        if (BackpackUi.accessories && CuriosPresence.loaded()) {
            for (CurioHit hit : curioHits) {
                if (hoverRect((int) mouseX, (int) mouseY, hit.x, hit.y, InventoryLayout.ITEM, InventoryLayout.ITEM)) {
                    PacketDistributor.sendToServer(new InventoryPayloads.CurioClickPayload(hit.identifier, hit.index));
                    return true;
                }
            }
            for (LegacyHit hit : legacyHits) {
                if (hoverRect((int) mouseX, (int) mouseY, hit.x, hit.y, InventoryLayout.ITEM, InventoryLayout.ITEM)) {
                    PacketDistributor.sendToServer(new InventoryPayloads.AccessoryClickPayload(hit.index));
                    return true;
                }
            }
        }
        int slot = geometricSlot((int) mouseX, (int) mouseY);
        if (slot >= 0) return pickup(slot, button);
        swallowRelease = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int geometricSlot(int mouseX, int mouseY) {
        int cell = hoveredGridCell(mouseX, mouseY);
        if (cell >= 0) {
            int owner = menu.backpack().ownerOf(cell);
            return owner >= 0 ? owner : cell;
        }
        for (int i = 0; i < 9; i++) {
            int hx = leftPos + InventoryLayout.GRID_X + i * InventoryLayout.CELL;
            if (hoverRect(mouseX, mouseY, hx, topPos + InventoryLayout.HOTBAR_Y, InventoryLayout.CELL, InventoryLayout.CELL)) {
                return PlayerBackpackMenu.HOTBAR_START + i;
            }
        }
        if (BackpackUi.accessories) {
            if (CuriosPresence.loaded()) return -1;
            for (int i = 0; i < PlayerBackpackData.ACCESSORY_COUNT; i++) {
                int ax = leftPos + InventoryLayout.ACCESSORY_X + (i % 3) * InventoryLayout.CELL;
                int ay = topPos + InventoryLayout.ACCESSORY_Y + (i / 3) * InventoryLayout.CELL;
                if (hoverRect(mouseX, mouseY, ax, ay, InventoryLayout.ITEM, InventoryLayout.ITEM)) {
                    return PlayerBackpackMenu.ACCESSORY_START + i;
                }
            }
            return -1;
        }
        if (!net.exmo.exworld.Config.decryptionMode) {
            if (hoverRect(mouseX, mouseY, leftPos + InventoryLayout.WEAPON1_X, topPos + InventoryLayout.WEAPON1_Y, InventoryLayout.WEAPON_WIDTH, InventoryLayout.ITEM)) {
                return PlayerBackpackMenu.WEAPON_START;
            }
            if (hoverRect(mouseX, mouseY, leftPos + InventoryLayout.WEAPON2_X, topPos + InventoryLayout.WEAPON2_Y, InventoryLayout.WEAPON_WIDTH, InventoryLayout.ITEM)) {
                return PlayerBackpackMenu.WEAPON_START + 1;
            }
        }
        int[] xs = {InventoryLayout.HELMET_X, InventoryLayout.CHEST_X, InventoryLayout.LEGS_X, InventoryLayout.BOOTS_X};
        int[] ys = {InventoryLayout.HELMET_Y, InventoryLayout.CHEST_Y, InventoryLayout.LEGS_Y, InventoryLayout.BOOTS_Y};
        for (int i = 0; i < 4; i++) {
            if (hoverRect(mouseX, mouseY, leftPos + xs[i], topPos + ys[i], InventoryLayout.ITEM, InventoryLayout.ITEM)) {
                return PlayerBackpackMenu.ARMOR_START + i;
            }
        }
        if (hoverRect(mouseX, mouseY, leftPos + InventoryLayout.CORE_X, topPos + InventoryLayout.CORE_Y, InventoryLayout.ITEM, InventoryLayout.ITEM)) {
            return PlayerBackpackMenu.CORE_SLOT;
        }
        return -1;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingBar && accessoryMax > 0) {
            int bar = Math.max(12, viewH * viewH / (viewH + accessoryMax));
            int travel = Math.max(1, viewH - bar);
            accessoryScroll = Mth.clamp((int) ((mouseY - viewY - bar / 2.0) / travel * accessoryMax), 0, accessoryMax);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingBar = false;
        if (button == 0 && swallowRelease) {
            swallowRelease = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
        if (BackpackUi.accessories && CuriosPresence.loaded()
                && hoverRect((int) mouseX, (int) mouseY, viewX, viewY, viewW, viewH)) {
            accessoryScroll = Mth.clamp(accessoryScroll - (int) Math.signum(scrollY) * 12, 0, accessoryMax);
            return true;
        }
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
        int slot = geometricSlot((int) xMouse, (int) yMouse);
        return slot >= 0 ? slot : hoveredSlot == null ? -1 : hoveredSlot.index;
    }

    private record CurioHit(String identifier, int index, int x, int y) {}
    private record LegacyHit(int index, int x, int y) {}
}
