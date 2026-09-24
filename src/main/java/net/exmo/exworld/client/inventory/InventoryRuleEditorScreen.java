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
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** GUI editor for item footprint rules, mirroring PetiteInventory's edit-mode behaviour. */
public final class InventoryRuleEditorScreen extends Screen {
    private static final ItemFootprint[] SIZES = {
            ItemFootprint.UNIT, ItemFootprint.of(1, 2), ItemFootprint.of(1, 3),
            ItemFootprint.of(2, 1), ItemFootprint.of(2, 2), ItemFootprint.of(2, 3),
            ItemFootprint.of(3, 1), ItemFootprint.of(3, 2), ItemFootprint.of(3, 3)
    };

    private final Map<String, ItemFootprint> rules;
    private final List<String> ids;
    private EditBox idField;
    private int sizeIndex;
    private int scroll;

    public InventoryRuleEditorScreen(Map<String, ItemFootprint> rules) {
        super(Component.translatable("screen.exworld.footprint_editor"));
        this.rules = Map.copyOf(rules == null ? Map.of() : rules);
        this.ids = new ArrayList<>(this.rules.keySet());
        this.ids.sort(Comparator.naturalOrder());
    }

    protected void init() {
        int left = panelX();
        int top = panelY();
        int w = InventoryLayout.IMAGE_WIDTH;
        idField = new EditBox(font, left + 8, top + 28, w - 16, 18, Component.translatable("screen.exworld.footprint_item"));
        idField.setMaxLength(128);
        addRenderableWidget(idField);
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.footprint_from_hand"), b -> fromHand())
                .bounds(left + 8, top + 50, 72, 18).build());
        addRenderableWidget(Button.builder(Component.literal(SIZES[sizeIndex].token()), b -> {
            sizeIndex = (sizeIndex + 1) % SIZES.length;
            rebuildWidgets();
        }).bounds(left + 84, top + 50, 48, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.footprint_apply"), b -> apply())
                .bounds(left + 136, top + 50, 48, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + w - 56, top + 50, 48, 18).build());

        int listTop = top + 76;
        int listBottom = top + InventoryLayout.IMAGE_HEIGHT - 8;
        int y = listTop - scroll;
        for (String id : ids) {
            if (y + 16 > listTop && y < listBottom) {
                int rowY = y;
                addRenderableWidget(Button.builder(Component.literal("×"), b -> remove(id))
                        .bounds(left + w - 26, rowY, 18, 16).build());
            }
            y += 20;
        }
    }

    private int panelX() {
        return (width - InventoryLayout.IMAGE_WIDTH) / 2;
    }

    private int panelY() {
        return (height - InventoryLayout.IMAGE_HEIGHT) / 2;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, InventoryLayout.PANEL);
        int x = panelX();
        int y = panelY();
        int w = InventoryLayout.IMAGE_WIDTH;
        int h = InventoryLayout.IMAGE_HEIGHT;
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, InventoryLayout.LINE);
        graphics.fill(x, y, x + w, y + h, InventoryLayout.SURFACE);
        graphics.fill(x, y + 72, x + w, y + 73, InventoryLayout.LINE_INNER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = panelX();
        int top = panelY();
        int w = InventoryLayout.IMAGE_WIDTH;
        graphics.drawCenteredString(font, title, left + w / 2, top + 8, InventoryLayout.TEXT);

        int listTop = top + 76;
        int listBottom = top + InventoryLayout.IMAGE_HEIGHT - 8;
        graphics.enableScissor(left + 2, listTop, left + w - 2, listBottom);
        int y = listTop + 4 - scroll;
        for (String id : ids) {
            ItemFootprint footprint = rules.get(id);
            String size = footprint == null ? "1x1" : footprint.token();
            graphics.drawString(font, displayName(id), left + 10, y + 1, InventoryLayout.TEXT);
            graphics.drawString(font, size, left + w - 70, y + 1, InventoryLayout.MUTED);
            y += 20;
        }
        graphics.disableScissor();
        if (ids.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.exworld.footprint_empty"), left + w / 2, listTop + 8, InventoryLayout.MUTED);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listH = InventoryLayout.IMAGE_HEIGHT - 84;
        int maxScroll = Math.max(0, ids.size() * 20 - listH);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (scrollY * 12)));
        rebuildWidgets();
        return true;
    }


    private String displayName(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return id;
        Item item = BuiltInRegistries.ITEM.get(location);
        if (item == null || item == net.minecraft.world.item.Items.AIR) return id;
        return item.getDescription().getString() + "  (" + id + ")";
    }

    private void fromHand() {
        if (minecraft == null || minecraft.player == null) return;
        var stack = minecraft.player.getMainHandItem();
        if (!stack.isEmpty()) {
            idField.setValue(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
    }

    private void apply() {
        String id = idField.getValue().trim();
        if (id.isBlank()) return;
        PacketDistributor.sendToServer(new InventoryPayloads.FootprintEditPayload(id, SIZES[sizeIndex].token(), false));
    }

    private void remove(String id) {
        PacketDistributor.sendToServer(new InventoryPayloads.FootprintEditPayload(id, "", true));
    }
}
