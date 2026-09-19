package net.exmo.exworld.client.tooltip;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Per-frame capture of the vanilla tooltip call so the mixin can hand off to the themed renderer. */
public final class TooltipCapture {
    private static ItemStack stack = ItemStack.EMPTY;
    private static List<net.minecraft.network.chat.Component> lines = List.of();
    private static int scroll;
    private static int maxScroll;
    private static boolean showing;
    private static boolean renderedThisFrame;

    private TooltipCapture() {}

    public static void begin(ItemStack item) {
        begin(item, List.of());
    }

    public static void begin(ItemStack item, List<net.minecraft.network.chat.Component> tooltipLines) {
        ItemStack next = item == null ? ItemStack.EMPTY : item;
        if (!ItemStack.isSameItemSameComponents(stack, next)) {
            scroll = 0;
            lines = List.of();
        }
        stack = next;
        if (tooltipLines != null && !tooltipLines.isEmpty()) {
            lines = List.copyOf(tooltipLines);
        }
    }

    public static ItemStack stack() {
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public static List<net.minecraft.network.chat.Component> lines() {
        return lines;
    }

    public static int scroll() {
        return scroll;
    }

    public static void setMaxScroll(int value) {
        maxScroll = Math.max(0, value);
        if (scroll > maxScroll) scroll = maxScroll;
    }

    public static void markRendered() {
        showing = true;
        renderedThisFrame = true;
    }

    public static boolean showing() {
        return showing;
    }

    public static boolean scrollBy(double deltaY) {
        if (!showing || maxScroll <= 0) return false;
        int step = (int) Math.round(deltaY * TooltipLayout.LINE);
        if (step == 0) step = deltaY > 0 ? TooltipLayout.LINE : -TooltipLayout.LINE;
        int next = Math.max(0, Math.min(maxScroll, scroll - step));
        if (next == scroll) return true;
        scroll = next;
        return true;
    }

    public static void end() {
        // Stack is kept until the next frame so scroll events still see the open tooltip.
    }

    public static void tick() {
        if (!renderedThisFrame) {
            showing = false;
            maxScroll = 0;
            stack = ItemStack.EMPTY;
            lines = List.of();
        }
        renderedThisFrame = false;
    }
}
