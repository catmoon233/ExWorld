package net.exmo.exworld.mystery.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Rewind-safe personal knowledge view. */
public final class MysteryJournalScreen extends Screen {
    private int scroll;
    public MysteryJournalScreen() { super(Component.translatable("screen.exworld.mystery_journal")); }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xEF111A24);
        graphics.fill(width / 2 - 155, 20, width / 2 + 155, height - 20, 0xFF2A303D);
        graphics.drawCenteredString(font, title, width / 2, 38, 0xFFFFE4AD);
        graphics.drawCenteredString(font, MysteryClient.displayName(), width / 2, 56, 0xFFC9D2DD);
        String[] clues = MysteryClient.knowledge().isBlank() ? new String[0] : MysteryClient.knowledge().split("\n");
        int y = 80;
        if (clues.length == 0) graphics.drawCenteredString(font,
                Component.translatable("screen.exworld.mystery_empty"), width / 2, y, 0xFFC9D2DD);
        int visible = Math.max(1, (height - 126) / 18);
        scroll = Math.min(scroll, Math.max(0, clues.length - visible));
        for (int i = scroll; i < Math.min(clues.length, scroll + visible); i++) {
            String clue = clues[i];
            String key = "clue.exworld." + clue.replace(':', '.');
            graphics.drawString(font, font.plainSubstrByWidth("• " + Component.translatable(key).getString(), 280),
                    width / 2 - 140, y, 0xFFE9DCA9, false);
            y += 18;
        }
        graphics.drawCenteredString(font, Component.translatable("screen.exworld.mystery_close"), width / 2, height - 34, 0xFF98A8B8);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int count = MysteryClient.knowledge().isBlank() ? 0 : MysteryClient.knowledge().split("\n").length;
        int visible = Math.max(1, (height - 126) / 18);
        scroll = Math.max(0, Math.min(Math.max(0, count - visible), scroll - (int)Math.signum(scrollY)));
        return true;
    }
}
