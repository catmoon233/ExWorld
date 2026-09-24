package net.exmo.exworld.client.tooltip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;


/** Draws panel chrome, badges, section headers, glow and animated item icons for the themed tooltip. */
public final class TooltipPainter {
    private TooltipPainter() {}

    public static void drawGlow(GuiGraphics graphics, int x, int y, int width, int height, int accent, float fade) {
        int outer = withAlpha(RarityPalette.brighten(accent, 0.30f), 0.14f * fade);
        int inner = withAlpha(accent, 0.06f * fade);
        drawRect(graphics, x - 2, y - 2, width + 4, height + 4, inner);
        drawRect(graphics, x - 1, y - 1, width + 2, height + 2, outer);
    }

    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height,
                                 TooltipTheme theme, int accent, float fade, long timeMs) {
        TooltipTheme t = theme.withAlpha(fade);
        fillGradient(graphics, x, y, width, height, t.bgTop(), t.bgBottom());
        int headerH = TooltipLayout.PAD + TooltipLayout.headerHeight();
        int hTop = withAlpha(RarityPalette.brighten(accent, 0.12f), 0.22f * fade);
        int hBottom = withAlpha(RarityPalette.darken(accent, 0.30f), 0.18f * fade);
        fillGradient(graphics, x, y, width, headerH, hTop, hBottom);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, t.borderInner());
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, t.borderInner());
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, t.borderInner());
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, t.borderInner());
        drawRect(graphics, x, y, width, height, t.border());
        drawFlow(graphics, x, y, width, height, t.flow(), timeMs);
    }

    public static void drawSlot(GuiGraphics graphics, int x, int y, TooltipTheme theme, float fade) {
        TooltipTheme t = theme.withAlpha(fade);
        int size = TooltipLayout.SLOT;
        graphics.fill(x, y, x + size, y + size, t.slotFill());
        drawRect(graphics, x, y, size, size, t.borderInner());
    }

    public static void drawDiamondFrame(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int r = Math.max(2, radius);
        graphics.fill(centerX, centerY - r, centerX + 1, centerY - r + 2, color);
        graphics.fill(centerX, centerY + r - 2, centerX + 1, centerY + r, color);
        graphics.fill(centerX - r, centerY, centerX - r + 2, centerY + 1, color);
        graphics.fill(centerX + r - 2, centerY, centerX + r, centerY + 1, color);
    }

    public static int drawBadge(GuiGraphics graphics, Font font, String label, int x, int y,
                                int bg, int fg, int clipLeft, int clipRight) {
        int width = TooltipLayout.chipWidth(font.width(label));
        int end = x + width;
        int left = Math.max(x, clipLeft);
        int right = Math.min(end, clipRight);
        if (left < right) {
            boolean clipped = left != x || right != end;
            if (clipped) graphics.enableScissor(left, y, right, y + TooltipLayout.LINE);
            graphics.fill(x, y, end, y + TooltipLayout.LINE, bg);
            graphics.drawString(font, label, x + TooltipLayout.CHIP_PAD_H, y, fg, false);
            if (clipped) graphics.disableScissor();
        }
        return end;
    }

    public static void drawSeparator(GuiGraphics graphics, int x, int y, int width, int accent, float fade) {
        int to = withAlpha(accent, 0.60f * fade);
        int from = withAlpha(accent, 0.04f * fade);
        for (int i = 0; i < width; i++) {
            float t = 1.0f - Math.abs(i - width / 2.0f) / (width / 2.0f);
            graphics.fill(x + i, y, x + i + 1, y + 1, RarityPalette.lerp(from, to, t));
        }
    }

    public static void drawSectionHeader(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        graphics.drawString(font, Component.literal("◆ " + text), x, y, color, false);
    }

    public static void drawBodyLine(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        Component line = Component.literal(text).withStyle(Style.EMPTY.withColor(color));
        graphics.drawString(font, line, x, y, 0xFFFFFFFF, false);
    }

    /** Draws a vanilla line keeping its original style colours; {@code defaultColor} applies only when unstyled. */
    public static void drawComponent(GuiGraphics graphics, Font font, Component component, int x, int y, int defaultColor) {
        int base = component.getStyle().getColor() != null ? 0xFFFFFFFF : defaultColor;
        graphics.drawString(font, component, x, y, base, false);
    }

    /** simplytooltips-style staggered square pip for the Slots section. */
    public static void drawAnimatedPip(GuiGraphics graphics, int x, int y, int size,
                                       int color, int topHighlight, long timeMs, int seqIdx) {
        long t = timeMs - seqIdx * 40L;
        float scale = t <= 0 ? 0.6f : t >= 120 ? 1f : 0.6f + 0.4f * easeOutCubic(t / 120f);
        int half = Math.max(1, (int) Math.ceil(size * scale / 2.0));
        int cx = x + size / 2;
        int cy = y + size / 2;
        graphics.fill(cx - half, cy - half, cx + half, cy + half, color);
        graphics.fill(cx - half, cy - half, cx + half, cy - half + 1, topHighlight);
    }

    public static void drawFooterDots(GuiGraphics graphics, int centerX, int y, TooltipTheme theme, int accent, float fade, long timeMs) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(timeMs * 0.004);
        for (int i = 0; i < 3; i++) {
            int color = i == 1
                    ? RarityPalette.lerp(theme.borderInner(), accent, pulse)
                    : withAlpha(theme.borderInner(), 0.8f * fade);
            graphics.fill(centerX - 7 + i * 7, y, centerX - 5 + i * 7, y + 2, color);
        }
    }

    /**
     * In-place icon. GuiGraphics.renderItem adds a z offset after the caller pose, so a Y spin
     * turns that offset into a screen orbit. Render the model at the slot centre ourselves.
     */
    public static void drawAnimatedItem(GuiGraphics graphics, ItemStack stack, int centerX, int centerY,
                                        long timeMs, boolean equipment) {
        if (stack == null || stack.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        var renderer = minecraft.getItemRenderer();
        BakedModel model = renderer.getModel(stack, minecraft.level, minecraft.player, 0);
        int half = TooltipLayout.SLOT / 2;
        graphics.enableScissor(centerX - half, centerY - half, centerX + half, centerY + half);
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 0.0F);
        if (model.isGui3d()) {
            float yaw = (timeMs % 8000L) / 8000.0F * 360.0F;
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.mulPose(Axis.XP.rotationDegrees(25.0F));
        } else if (equipment) {
            float tilt = (float) (Math.sin(timeMs * 0.0018) * 4.0);
            pose.mulPose(Axis.ZP.rotationDegrees(tilt));
        }
        pose.scale(16.0F, -16.0F, 16.0F);
        boolean flatLight = !model.usesBlockLight();
        if (flatLight) Lighting.setupForFlatItems();
        else Lighting.setupFor3DItems();
        renderer.render(stack, ItemDisplayContext.GUI, false, pose, graphics.bufferSource(),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
        graphics.flush();
        pose.popPose();
        graphics.disableScissor();
        if (flatLight) Lighting.setupFor3DItems();
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
            graphics.fill(x, y + i, x + width, y + i + 1, RarityPalette.lerp(top, bottom, t));
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

    private static float easeOutCubic(float t) {
        float x = Math.max(0f, Math.min(1f, t));
        return 1.0f - (1.0f - x) * (1.0f - x) * (1.0f - x);
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
