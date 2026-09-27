package net.exmo.exworld.client.inventory;

import net.exmo.exworld.client.tooltip.RarityPalette;
import net.exmo.exworld.client.tooltip.TooltipTheme;
import net.minecraft.client.gui.GuiGraphics;

/** Backpack chrome that follows the common-rarity tooltip panel. */
public final class InventoryChrome {
    private InventoryChrome() {}

    public static float fade(long openMillis) {
        float t = Math.max(0f, Math.min(1f, (System.currentTimeMillis() - openMillis) / 200f));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    public static TooltipTheme theme() {
        return RarityPalette.themeOf(RarityPalette.COMMON);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, float fade) {
        if (width <= 0 || height <= 0) return;
        TooltipTheme theme = theme();
        int glow = alpha(RarityPalette.COMMON, 0.14f * fade);
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, glow);
        fillGradient(graphics, x, y, width, height, alpha(theme.bgTop(), fade), alpha(theme.bgBottom(), fade));
        int header = Math.min(28, height);
        fillGradient(graphics, x, y, width, header,
                alpha(RarityPalette.brighten(RarityPalette.COMMON, 0.12f), 0.22f * fade),
                alpha(RarityPalette.darken(RarityPalette.COMMON, 0.30f), 0.10f * fade));
        stroke(graphics, x, y, width, height, alpha(theme.border(), fade));
        if (width > 2 && height > 2) {
            stroke(graphics, x + 1, y + 1, width - 2, height - 2, alpha(theme.borderInner(), fade));
        }
        drawFlow(graphics, x, y, width, height, alpha(theme.flow(), 0.85f * fade));
    }

    public static void scrollbar(GuiGraphics graphics, int x, int y, int height, int maxScroll, int scroll) {
        if (maxScroll <= 0 || height <= 0) return;
        graphics.fill(x, y, x + 3, y + height, alpha(theme().borderInner(), 0.7f));
        int bar = Math.max(12, height * height / (height + maxScroll));
        int barY = y + (int) ((height - bar) * (scroll / (float) maxScroll));
        graphics.fill(x, barY, x + 3, barY + bar, theme().border());
    }

    private static void drawFlow(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        int perimeter = Math.max(1, 2 * (width + height));
        int pos = (int) ((System.currentTimeMillis() / 16L) % perimeter);
        int length = Math.max(10, Math.min(28, Math.min(width, height) / 3));
        for (int i = 0; i < length; i++) {
            int at = (pos + i) % perimeter;
            int tint = (color & 0x00FFFFFF) | ((40 + (180 * i / length)) << 24);
            place(graphics, x, y, width, height, at, tint);
        }
    }

    private static void place(GuiGraphics graphics, int x, int y, int width, int height, int pos, int color) {
        int top = width;
        int right = top + height;
        int bottom = right + width;
        if (pos < top) graphics.fill(x + pos, y, x + pos + 1, y + 1, color);
        else if (pos < right) graphics.fill(x + width - 1, y + pos - top, x + width, y + pos - top + 1, color);
        else if (pos < bottom) graphics.fill(x + width - 1 - (pos - right), y + height - 1, x + width - (pos - right), y + height, color);
        else graphics.fill(x, y + height - 1 - (pos - bottom), x + 1, y + height - (pos - bottom), color);
    }

    private static void fillGradient(GuiGraphics graphics, int x, int y, int width, int height, int top, int bottom) {
        for (int i = 0; i < height; i++) {
            float t = height == 1 ? 0f : i / (float) (height - 1);
            graphics.fill(x, y + i, x + width, y + i + 1, RarityPalette.lerp(top, bottom, t));
        }
    }

    private static void stroke(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static int alpha(int color, float fade) {
        int a = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 0xFF) * fade)));
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
