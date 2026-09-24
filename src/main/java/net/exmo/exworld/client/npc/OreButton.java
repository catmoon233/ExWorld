package net.exmo.exworld.client.npc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Ore-style action control. It replaces the vanilla button face without using chat click events. */
public final class OreButton extends Button {
    public enum Kind { PRIMARY, SECONDARY, DANGER, SURFACE }

    private final Kind kind;
    private boolean marked;

    private OreButton(int x, int y, int width, int height, Component message, OnPress onPress, Kind kind) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.kind = kind == null ? Kind.SURFACE : kind;
    }

    public static OreButton of(int x, int y, int width, int height, Component message, OnPress onPress, Kind kind) {
        return new OreButton(x, y, width, height, message, onPress, kind);
    }

    public OreButton marked(boolean marked) {
        this.marked = marked;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int face = switch (kind) {
            case PRIMARY -> OreChrome.GREEN;
            case SECONDARY -> OreChrome.SURFACE;
            case DANGER -> OreChrome.SURFACE;
            case SURFACE -> marked ? OreChrome.GREEN : OreChrome.SURFACE;
        };
        int shadow = kind == Kind.PRIMARY ? OreChrome.GREEN_HOVER : kind == Kind.DANGER ? 0xFFFFF1F0 : OreChrome.DEEP;
        if (!active) face = OreChrome.DEEP;
        OreChrome.button(graphics, getX(), getY(), width, height, face, shadow, isHovered(), false, marked && kind != Kind.SURFACE);
        int color = !active ? OreChrome.MUTED : kind == Kind.PRIMARY || (kind == Kind.SURFACE && marked) ? OreChrome.WHITE : kind == Kind.DANGER ? OreChrome.RED : kind == Kind.SECONDARY ? OreChrome.PURPLE : OreChrome.INK;
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, color);
    }
}
