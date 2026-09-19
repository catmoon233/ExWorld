package net.exmo.exworld.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Draws panel chrome, badges, and section headers for the themed tooltip. */
public final class TooltipPainter {
    private TooltipPainter() {}

    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, TooltipTheme theme, long timeMs) {
        fillGradient(graphics, x, y, width, height, theme.bgTop(), theme.bgBottom());
        int headerH = TooltipLayout.PAD + TooltipLayout.headerHeight();
        graphics.fill(x, y, x + width, y + headerH, theme.titleBar());
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, theme.borderInner());
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, theme.borderInner());
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, theme.borderInner());
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, theme.borderInner());
        drawRect(graphics, x, y, width, height, theme.border());
        drawFlow(graphics, x, y, width, height, theme.flow(), timeMs);
    }

    public static void drawSlot(GuiGraphics graphics, int x, int y, TooltipTheme theme) {
        int size = TooltipLayout.SLOT;
        graphics.fill(x, y, x + size, y + size, theme.slotFill());
        drawRect(graphics, x, y, size, size, theme.borderInner());
    }

    public static int drawBadge(GuiGraphics graphics, Font font, String label, int x, int y, int bg, int fg) {
        int width = TooltipLayout.chipWidth(font.width(label));
        graphics.fill(x, y, x + width, y + TooltipLayout.LINE, bg);
        graphics.drawString(font, label, x + TooltipLayout.CHIP_PAD_H, y, fg & 0x00FFFFFF | 0xFF000000, false);
        return x + width;
    }

    public static void drawSeparator(GuiGraphics graphics, int x, int y, int width, int color) {
        int mid = x + width / 2;
        graphics.fill(x, y, mid, y + 1, color & 0x00FFFFFF);
        graphics.fill(mid, y, x + width, y + 1, color);
    }

    public static void drawSectionHeader(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        graphics.drawString(font, Component.literal("◆ " + text), x, y, color, false);
    }

    public static void drawBodyLine(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, false);
    }

    public static void drawItemIcon(GuiGraphics graphics, ItemStack stack, int slotX, int slotY) {
        int inset = (TooltipLayout.SLOT - TooltipLayout.ICON) / 2;
        graphics.renderItem(stack, slotX + inset, slotY + inset);
    }

    private static void drawRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static void fillGradient(GuiGraphics graphics, int x, int y, int width, int height, int top, int bottom) {
        if (width <= 0 || height <= 0) return;
        for (int i = 0; i < height; i++) {
            float t = height == 1 ? 0f : i / (float) (height - 1);
            graphics.fill(x, y + i, x + width, y + i + 1, lerp(top, bottom, t));
        }
    }

    private static void drawFlow(GuiGraphics graphics, int x, int y, int width, int height, int color, long timeMs) {
        int perimeter = Math.max(1, 2 * (width + height));
        int pos = (int) ((timeMs / 12L) % perimeter);
        int length = Math.max(12, Math.min(width, height));
        for (int i = 0; i < length; i++) {
            placeOnPerimeter(graphics, x, y, width, height, (pos + i) % perimeter, color);
        }
    }

    private static void placeOnPerimeter(GuiGraphics graphics, int x, int y, int width, int height, int pos, int color) {
        int top = width;
        int right = top + height;
        int bottom = right + width;
        if (pos < top) {
            graphics.fill(x + pos, y, x + pos + 1, y + 1, color);
        } else if (pos < right) {
            int p = pos - top;
            graphics.fill(x + width - 1, y + p, x + width, y + p + 1, color);
        } else if (pos < bottom) {
            int p = pos - right;
            graphics.fill(x + width - 1 - p, y + height - 1, x + width - p, y + height, color);
        } else {
            int p = pos - bottom;
            graphics.fill(x, y + height - 1 - p, x + 1, y + height - p, color);
        }
    }

    private static int lerp(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int ra = (int) (aa + (ba - aa) * t);
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }
}
