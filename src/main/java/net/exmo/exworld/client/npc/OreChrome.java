package net.exmo.exworld.client.npc;

import net.minecraft.client.gui.GuiGraphics;

/** WeChat-style chrome. Names stay so existing screens pick up the new palette. */
public final class OreChrome {
    public static final int CANVAS = 0xFFF7F7F7;
    public static final int SURFACE = 0xFFFFFFFF;
    public static final int DEEP = 0xFFEDEDED;
    public static final int SOFT = 0xFF191919;
    public static final int EDGE = 0xFFE5E5E5;
    public static final int EDGE_LIGHT = 0xFFD0D0D0;
    public static final int INK = 0xFF191919;
    public static final int MUTED = 0xFF8A8A8A;
    public static final int INK_DARK = 0xFF191919;
    public static final int GREEN = 0xFF07C160;
    public static final int GREEN_HOVER = 0xFF06AD56;
    public static final int GREEN_SHADOW = 0xFF06AD56;
    public static final int PURPLE = 0xFF576B95;
    public static final int GOLD = 0xFF07C160;
    public static final int RED = 0xFFFA5151;
    public static final int RED_HOVER = 0xFFE64340;
    public static final int OVERLAY = 0x66000000;
    public static final int WHITE = 0xFFFFFFFF;

    private OreChrome() {}

    public static void panel(GuiGraphics graphics, int x, int y, int w, int h, int accent) {
        graphics.fill(x, y, x + w, y + h, SURFACE);
        graphics.fill(x, y, x + w, y + 1, EDGE);
        graphics.fill(x, y + h - 1, x + w, y + h, EDGE);
        graphics.fill(x, y, x + 1, y + h, EDGE);
        graphics.fill(x + w - 1, y, x + w, y + h, EDGE);
        if (accent != 0) graphics.fill(x, y, x + 3, y + h, accent);
    }

    public static void header(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, SURFACE);
        graphics.fill(x, y + h - 1, x + w, y + h, EDGE);
    }

    public static void well(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, SURFACE);
        graphics.fill(x, y + h - 1, x + w, y + h, EDGE);
    }

    public static void button(GuiGraphics graphics, int x, int y, int w, int h, int face, int shadow, boolean hover, boolean pressed, boolean marked) {
        int body = pressed ? shadow : hover ? shift(face) : face;
        graphics.fill(x, y, x + w, y + h, body);
        if (marked) graphics.fill(x, y, x + 3, y + h, GREEN);
    }

    public static void bubble(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + h, color);
    }

    private static int shift(int color) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + 12);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 12);
        int b = Math.min(255, (color & 0xFF) + 12);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
