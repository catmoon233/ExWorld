package net.exmo.exworld.client.quest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A texture-backed journal control; it deliberately never falls back to the vanilla grey button. */
final class JournalButton extends AbstractButton {
    @FunctionalInterface
    interface PressAction { void press(JournalButton button); }

    private final PressAction action;
    private boolean selected;

    JournalButton(int x, int y, int width, int height, Component message, PressAction action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    JournalButton selected(boolean value) {
        selected = value;
        return this;
    }

    @Override public void onPress() { action.press(this); }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int drawY = getY() - (active && isHoveredOrFocused() ? 1 : 0);
        JournalGuiTextures.panel(graphics, getX(), drawY, getWidth(), getHeight());
        int color = !active ? 0xFF8A6540 : selected ? 0xFFFFE5A4 : isHoveredOrFocused() ? 0xFFFFD574 : 0xFF4B351C;
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + getWidth() / 2,
                drawY + Math.max(2, (getHeight() - 8) / 2), color);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) { output.add(NarratedElementType.TITLE, getMessage()); }
}
