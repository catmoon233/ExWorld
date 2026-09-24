package net.exmo.exworld.client.social;

import net.exmo.exworld.client.npc.OreButton;
import net.exmo.exworld.client.npc.OreChrome;
import net.exmo.exworld.network.NearbyChatUpdatePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Configures how far adventure-mode speech carries. */
public final class NearbyChatScreen extends Screen {
    private static final int PANEL_W = 280;
    private static final int PANEL_H = 196;

    private final int distance;
    private final int range;
    private EditBox distanceBox;
    private EditBox rangeBox;

    public NearbyChatScreen(int distance, int range) {
        super(Component.translatable("screen.exworld.nearby_chat"));
        this.distance = distance;
        this.range = range;
    }

    @Override
    protected void init() {
        int x = panelX();
        int y = panelY();
        distanceBox = field(x + 108, y + 40, distance);
        rangeBox = field(x + 108, y + 68, range);
        addRenderableWidget(distanceBox);
        addRenderableWidget(rangeBox);
        addRenderableWidget(OreButton.of(x + 24, y + 156, 108, 24, Component.translatable("screen.exworld.nearby_save"),
                pressed -> save(), OreButton.Kind.PRIMARY));
        addRenderableWidget(OreButton.of(x + 148, y + 156, 108, 24, Component.translatable("npc.exworld.close"),
                pressed -> onClose(), OreButton.Kind.SURFACE));
    }

    private EditBox field(int x, int y, int value) {
        EditBox box = new EditBox(font, x, y, 140, 18, Component.empty());
        box.setValue(Integer.toString(value));
        box.setFilter(NearbyChatScreen::digits);
        box.setMaxLength(4);
        return box;
    }

    private void save() {
        PacketDistributor.sendToServer(new NearbyChatUpdatePayload(parse(distanceBox, distance), parse(rangeBox, range)));
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OreChrome.OVERLAY);
        OreChrome.panel(graphics, panelX(), panelY(), PANEL_W, PANEL_H, OreChrome.GREEN);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = panelX();
        int y = panelY();
        graphics.drawString(font, title, x + 16, y + 14, OreChrome.INK, false);
        graphics.drawString(font, Component.translatable("screen.exworld.nearby_distance"), x + 16, y + 44, OreChrome.INK, false);
        graphics.drawString(font, Component.translatable("screen.exworld.nearby_range"), x + 16, y + 72, OreChrome.INK, false);
        int hintY = y + 98;
        for (var line : font.split(Component.translatable("screen.exworld.nearby_hint"), PANEL_W - 32)) {
            graphics.drawString(font, line, x + 16, hintY, OreChrome.MUTED, false);
            hintY += 10;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private int panelX() { return (width - PANEL_W) / 2; }
    private int panelY() { return (height - PANEL_H) / 2; }

    private static int parse(EditBox box, int fallback) {
        try {
            return Integer.parseInt(box.getValue().trim());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static boolean digits(String value) {
        if (value.isEmpty()) return true;
        for (int i = 0; i < value.length(); i++) if (!Character.isDigit(value.charAt(i))) return false;
        return true;
    }
}
