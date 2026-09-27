package net.exmo.lotm.client.story;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class StoryReadScreen extends Screen {
    private static final int PAPER = 0xFFF4EFE4;
    private static final int INK = 0xFF1C1C1E;
    private final String pagesKey;
    private int scroll;

    public StoryReadScreen(String pagesKey) {
        super(Component.translatable(pagesKey.replace(".pages", "")));
        this.pagesKey = pagesKey;
    }

    public static void open(String pagesKey) {
        Minecraft.getInstance().setScreen(new StoryReadScreen(pagesKey));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = width / 2 - 130;
        int top = height / 2 - 110;
        graphics.fill(left, top, left + 260, top + 220, PAPER);
        graphics.drawString(font, title, left + 14, top + 12, INK, false);
        List<FormattedCharSequence> lines = lines();
        int max = Math.max(0, (lines.size() - 12) * 11);
        if (scroll > max) scroll = max;
        graphics.enableScissor(left + 8, top + 28, left + 252, top + 208);
        int y = top + 32 - scroll;
        for (FormattedCharSequence line : lines) {
            if (y > top + 16 && y < top + 208) graphics.drawString(font, line, left + 14, y, INK, false);
            y += 11;
        }
        graphics.disableScissor();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private List<FormattedCharSequence> lines() {
        List<FormattedCharSequence> lines = new ArrayList<>();
        String raw = Component.translatable(pagesKey).getString();
        for (String paragraph : raw.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(font.split(Component.literal(paragraph), 228));
        }
        return lines;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * 12);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
