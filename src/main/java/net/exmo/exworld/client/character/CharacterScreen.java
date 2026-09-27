package net.exmo.exworld.client.character;

import net.exmo.exworld.client.inventory.BackpackTabs;
import net.exmo.exworld.client.inventory.InventoryChrome;
import net.exmo.exworld.inventory.InventoryLayout;
import net.exmo.exworld.network.InventoryPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Character sheet in the backpack window. The body is the imported markdown text. */
public final class CharacterScreen extends Screen {
    private static final int LINE = 11;
    private final String text;
    private int panelX;
    private int panelY;
    private int scroll;

    public CharacterScreen(String text) {
        super(Component.translatable("screen.exworld.character"));
        this.text = text == null ? "" : text;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        panelX = (width - InventoryLayout.IMAGE_WIDTH) / 2;
        panelY = (height - InventoryLayout.IMAGE_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = panelX;
        int y = panelY;
        drawTab(graphics, x, y, Component.translatable("screen.exworld.backpack_tab"), false,
                hit(mouseX, mouseY, x, y));
        int sequenceX = BackpackTabs.sequenceTabX(x);
        if (sequenceX >= 0) {
            drawTab(graphics, sequenceX, y, Component.translatable("screen.exworld.sequence_tab"), false,
                    hit(mouseX, mouseY, sequenceX, y));
        }
        int characterX = BackpackTabs.characterTabX(x);
        drawTab(graphics, characterX, y, Component.translatable("screen.exworld.character_tab"), true, false);
        int body = y + InventoryLayout.TAB_H;
        InventoryChrome.panel(graphics, x, body, InventoryLayout.IMAGE_WIDTH,
                InventoryLayout.IMAGE_HEIGHT - InventoryLayout.TAB_H, 1f);
        int textX = x + 10;
        int textY = body + 8;
        int textW = InventoryLayout.IMAGE_WIDTH - 20;
        int textH = InventoryLayout.IMAGE_HEIGHT - InventoryLayout.TAB_H - 16;
        graphics.enableScissor(textX, textY, textX + textW, textY + textH);
        if (text.isBlank()) {
            graphics.drawString(font, Component.translatable("screen.exworld.character_empty"),
                    textX, textY, InventoryLayout.MUTED, false);
        } else {
            int lineY = textY - scroll;
            for (FormattedCharSequence line : lines(textW)) {
                if (lineY + LINE >= textY && lineY <= textY + textH) {
                    graphics.drawString(font, line, textX, lineY, InventoryLayout.TEXT, false);
                }
                lineY += LINE;
            }
        }
        graphics.disableScissor();
    }

    private List<FormattedCharSequence> lines(int width) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String raw : text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            String shown = raw;
            if (shown.startsWith("#")) {
                int mark = 0;
                while (mark < shown.length() && shown.charAt(mark) == '#') mark++;
                if (mark < shown.length() && shown.charAt(mark) == ' ') shown = shown.substring(mark + 1);
            }
            if (shown.isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(font.split(Component.literal(shown), width));
        }
        return lines;
    }

    private void drawTab(GuiGraphics graphics, int x, int y, Component label, boolean active, boolean hovered) {
        int bg = active ? InventoryChrome.theme().titleBar() : hovered ? InventoryLayout.BUTTON_HOVER : InventoryLayout.TAB_IDLE;
        graphics.fill(x, y, x + BackpackTabs.TAB_W, y + InventoryLayout.TAB_H, bg);
        graphics.fill(x, y + InventoryLayout.TAB_H - 1, x + BackpackTabs.TAB_W, y + InventoryLayout.TAB_H,
                active ? InventoryChrome.theme().border() : InventoryLayout.LINE_INNER);
        graphics.drawCenteredString(font, label, x + BackpackTabs.TAB_W / 2, y + 7,
                active ? InventoryLayout.TEXT : InventoryLayout.MUTED);
    }

    private boolean hit(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + BackpackTabs.TAB_W && mouseY >= y && mouseY < y + InventoryLayout.TAB_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (hit(mouseX, mouseY, panelX, panelY)) {
            PacketDistributor.sendToServer(new InventoryPayloads.OpenBackpackPayload());
            return true;
        }
        int sequenceX = BackpackTabs.sequenceTabX(panelX);
        if (sequenceX >= 0 && hit(mouseX, mouseY, sequenceX, panelY)) {
            BackpackTabs.openSequence();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * LINE);
        return true;
    }
}
