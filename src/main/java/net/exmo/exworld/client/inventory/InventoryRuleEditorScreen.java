package net.exmo.exworld.client.inventory;

import net.exmo.exworld.inventory.InventoryLayout;
import net.exmo.exworld.inventory.ItemFootprint;
import net.exmo.exworld.network.InventoryPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Searchable footprint editor. Rows are drawn, not one button per item. */
public final class InventoryRuleEditorScreen extends Screen {
    private static final ItemFootprint[] SIZES = {
            ItemFootprint.UNIT, ItemFootprint.of(1, 2), ItemFootprint.of(1, 3),
            ItemFootprint.of(2, 1), ItemFootprint.of(2, 2), ItemFootprint.of(2, 3),
            ItemFootprint.of(3, 1), ItemFootprint.of(3, 2), ItemFootprint.of(3, 3)
    };
    private static final int ROW = 22;
    private static final int LIMIT = 80;

    private final Map<String, ItemFootprint> rules;
    private final List<String> ids;
    private EditBox search;
    private Button sizeButton;
    private int sizeIndex;
    private int scroll;
    private boolean dragging;
    private String cachedQuery = "\0";
    private List<String> visible = List.of();

    public InventoryRuleEditorScreen(Map<String, ItemFootprint> rules) {
        super(Component.translatable("screen.exworld.footprint_editor"));
        this.rules = Map.copyOf(rules == null ? Map.of() : rules);
        this.ids = new ArrayList<>(this.rules.keySet());
        this.ids.sort(Comparator.naturalOrder());
    }

    @Override
    protected void init() {
        int left = panelX();
        int top = panelY();
        int w = InventoryLayout.IMAGE_WIDTH;
        search = new EditBox(font, left + 8, top + 24, w - 16, 18, Component.translatable("screen.exworld.footprint_search"));
        search.setMaxLength(128);
        search.setHint(Component.translatable("screen.exworld.footprint_search"));
        search.setResponder(value -> {
            scroll = 0;
            cachedQuery = "\0";
        });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.footprint_from_hand"), b -> fromHand())
                .bounds(left + 8, top + 46, 72, 18).build());
        sizeButton = Button.builder(Component.literal(SIZES[sizeIndex].token()), b -> {
            sizeIndex = (sizeIndex + 1) % SIZES.length;
            sizeButton.setMessage(Component.literal(SIZES[sizeIndex].token()));
        }).bounds(left + 84, top + 46, 48, 18).build();
        addRenderableWidget(sizeButton);
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.footprint_apply"), b -> apply())
                .bounds(left + 136, top + 46, 48, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + w - 56, top + 46, 48, 18).build());
    }

    private int panelX() {
        return (width - InventoryLayout.IMAGE_WIDTH) / 2;
    }

    private int panelY() {
        return (height - InventoryLayout.IMAGE_HEIGHT) / 2;
    }

    private int listTop() {
        return panelY() + 72;
    }

    private int listH() {
        return InventoryLayout.IMAGE_HEIGHT - 80;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xC008080C);
        InventoryChrome.panel(graphics, panelX(), panelY(), InventoryLayout.IMAGE_WIDTH, InventoryLayout.IMAGE_HEIGHT, 1f);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = panelX();
        int top = panelY();
        int w = InventoryLayout.IMAGE_WIDTH;
        graphics.drawCenteredString(font, title, left + w / 2, top + 8, InventoryLayout.TEXT);
        drawSizePreview(graphics, left + 188, top + 46);

        List<String> rows = visible();
        int listTop = listTop();
        int listH = listH();
        int max = maxScroll(rows);
        scroll = Mth.clamp(scroll, 0, max);
        graphics.enableScissor(left + 4, listTop, left + w - 8, listTop + listH);
        int y = listTop + 2 - scroll;
        for (String id : rows) {
            if (y + ROW > listTop && y < listTop + listH) {
                boolean hover = mouseX >= left + 6 && mouseX < left + w - 28 && mouseY >= y && mouseY < y + ROW - 2
                        && mouseY >= listTop && mouseY < listTop + listH;
                if (hover || id.equals(search.getValue().trim())) {
                    graphics.fill(left + 6, y, left + w - 28, y + ROW - 2, hover ? InventoryLayout.BUTTON_HOVER : InventoryLayout.BUTTON);
                }
                drawIcon(graphics, id, left + 8, y + 2);
                ItemFootprint footprint = rules.get(id);
                String size = footprint == null ? "" : footprint.token();
                graphics.drawString(font, fit(displayName(id), w - 120), left + 28, y + 6, InventoryLayout.TEXT, false);
                if (!size.isEmpty()) graphics.drawString(font, size, left + w - 78, y + 6, InventoryLayout.MUTED, false);
                if (footprint != null) graphics.drawString(font, "×", left + w - 24, y + 6, InventoryLayout.TEXT, false);
            }
            y += ROW;
        }
        graphics.disableScissor();
        if (rows.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.exworld.footprint_empty"),
                    left + w / 2, listTop + 8, InventoryLayout.MUTED);
        }
        InventoryChrome.scrollbar(graphics, left + w - 6, listTop, listH, max, scroll);
    }

    private void drawSizePreview(GuiGraphics graphics, int x, int y) {
        ItemFootprint footprint = SIZES[sizeIndex];
        for (int row = 0; row < footprint.height(); row++) {
            for (int col = 0; col < footprint.width(); col++) {
                int cx = x + col * 5;
                int cy = y + row * 5;
                graphics.fill(cx, cy, cx + 4, cy + 4, InventoryLayout.LINE);
            }
        }
    }

    private void drawIcon(GuiGraphics graphics, String id, int x, int y) {
        Item item = item(id);
        if (item == null || item == Items.AIR) return;
        graphics.renderItem(new ItemStack(item), x, y);
    }

    private List<String> visible() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.equals(cachedQuery)) return visible;
        cachedQuery = query;
        List<String> rows = new ArrayList<>();
        if (query.isEmpty()) {
            rows.addAll(ids);
        } else {
            for (String id : ids) if (matches(id, query)) rows.add(id);
            int extra = 0;
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == null || item == Items.AIR) continue;
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                if (rules.containsKey(id) || !matches(id, query)) continue;
                rows.add(id);
                if (++extra >= LIMIT) break;
            }
        }
        visible = rows;
        return rows;
    }

    private boolean matches(String id, String query) {
        if (id.toLowerCase(Locale.ROOT).contains(query)) return true;
        return displayName(id).toLowerCase(Locale.ROOT).contains(query);
    }

    private String displayName(String id) {
        Item item = item(id);
        if (item == null || item == Items.AIR) return id;
        return item.getDescription().getString();
    }

    private Item item(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return Items.AIR;
        return BuiltInRegistries.ITEM.get(location);
    }

    private String fit(String text, int max) {
        if (font.width(text) <= max) return text;
        String ellipsis = "…";
        int limit = Math.max(0, max - font.width(ellipsis));
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end)) > limit) end--;
        return text.substring(0, end) + ellipsis;
    }

    private int maxScroll(List<String> rows) {
        return Math.max(0, rows.size() * ROW - listH());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overScrollbar(mouseX, mouseY)) {
            dragging = true;
            dragTo(mouseY);
            return true;
        }
        if (button == 0 && overList(mouseX, mouseY)) {
            int index = (int) ((mouseY - listTop() + scroll) / ROW);
            List<String> rows = visible();
            if (index >= 0 && index < rows.size()) {
                String id = rows.get(index);
                int left = panelX();
                int w = InventoryLayout.IMAGE_WIDTH;
                int rowY = listTop() + index * ROW - scroll;
                if (rules.containsKey(id) && mouseX >= left + w - 28 && mouseX < left + w - 10 && mouseY >= rowY && mouseY < rowY + ROW) {
                    PacketDistributor.sendToServer(new InventoryPayloads.FootprintEditPayload(id, "", true));
                    return true;
                }
                search.setValue(id);
                ItemFootprint footprint = rules.get(id);
                if (footprint != null) {
                    for (int i = 0; i < SIZES.length; i++) {
                        if (SIZES[i].width() == footprint.width() && SIZES[i].height() == footprint.height()) sizeIndex = i;
                    }
                    sizeButton.setMessage(Component.literal(SIZES[sizeIndex].token()));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            dragTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void dragTo(double mouseY) {
        int max = maxScroll(visible());
        if (max <= 0) return;
        int bar = Math.max(12, listH() * listH() / (listH() + max));
        int travel = Math.max(1, listH() - bar);
        scroll = Mth.clamp((int) ((mouseY - listTop() - bar / 2.0) / travel * max), 0, max);
    }

    private boolean overList(double mouseX, double mouseY) {
        return mouseX >= panelX() + 4 && mouseX < panelX() + InventoryLayout.IMAGE_WIDTH - 8
                && mouseY >= listTop() && mouseY < listTop() + listH();
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        return mouseX >= panelX() + InventoryLayout.IMAGE_WIDTH - 8 && mouseX < panelX() + InventoryLayout.IMAGE_WIDTH - 2
                && mouseY >= listTop() && mouseY < listTop() + listH();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!overList(mouseX, mouseY) && !overScrollbar(mouseX, mouseY)) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY) * ROW, 0, maxScroll(visible()));
        return true;
    }

    private void fromHand() {
        if (minecraft == null || minecraft.player == null) return;
        ItemStack stack = minecraft.player.getMainHandItem();
        if (!stack.isEmpty()) search.setValue(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private void apply() {
        String id = search.getValue().trim();
        if (id.isBlank()) return;
        PacketDistributor.sendToServer(new InventoryPayloads.FootprintEditPayload(id, SIZES[sizeIndex].token(), false));
    }
}
