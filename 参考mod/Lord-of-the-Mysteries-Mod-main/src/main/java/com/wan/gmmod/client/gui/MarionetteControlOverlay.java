package com.wan.gmmod.client.gui;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.MarionetteControlClientState;
import com.wan.gmmod.content.marionette.MarionetteAbilities;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 「共享视野」操控 HUD：
 * <ul>
 *   <li>屏幕边缘暗红色渐变遮罩，提示「正在操控秘偶」；</li>
 *   <li>视野四角绘制细微的灵体之线纹路（程序化线条 Overlay）；</li>
 *   <li>底部中央两个临时技能位（固定位置，不占用玩家技能栏）：
 *       左键·近战攻击 / 右键·原有能力（非远程秘偶时灰显）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public class MarionetteControlOverlay {
    /** 边缘遮罩厚度（像素） */
    private static final int VIGNETTE = 28;
    /** 暗红遮罩颜色（边缘不透明端） */
    private static final int DARK_RED = 0x90550000;
    /** 灵体之线颜色（半透明灵青色） */
    private static final int THREAD_COLOR = 0xAA77FFEE;
    /** 角落纹路长度（像素） */
    private static final int THREAD_LEN = 36;
    /** 临时技能位大小（像素） */
    private static final int SLOT = 22;

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(GuimiMod.id("marionette_control_hud"), MarionetteControlOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!MarionetteControlClientState.isControlling() || mc.options.hideGui) {
            return;
        }
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();

        // 1) 边缘暗红渐变遮罩（上下 / 左右四条）
        graphics.fillGradient(0, 0, w, VIGNETTE, DARK_RED, 0x00000000);
        graphics.fillGradient(0, h - VIGNETTE, w, h, 0x00000000, DARK_RED);
        fillGradientHorizontal(graphics, 0, 0, VIGNETTE, h, DARK_RED, 0x00000000);
        fillGradientHorizontal(graphics, w - VIGNETTE, 0, w, h, 0x00000000, DARK_RED);

        // 2) 四角灵体之线纹路：两段错位细线，似丝线缠绕
        drawCornerThreads(graphics, 6, 6, 1, 1);
        drawCornerThreads(graphics, w - 7, 6, -1, 1);
        drawCornerThreads(graphics, 6, h - 7, 1, -1);
        drawCornerThreads(graphics, w - 7, h - 7, -1, -1);

        // 3) 顶部提示文字
        Component title = Component.translatable("overlay.guimi_mod.marionette_control");
        graphics.drawCenteredString(mc.font, title, w / 2, VIGNETTE / 2, 0xFFCC5555);
        Component hint = Component.translatable("overlay.guimi_mod.marionette_control.hint");
        graphics.drawCenteredString(mc.font, hint, w / 2, h - VIGNETTE / 2 - mc.font.lineHeight, 0xFFAAAAAA);

        // 4) 底部中央临时技能位：左键近战 / 右键原有能力
        Mob mob = MarionetteControlClientState.getControlled();
        boolean hasAbility = mob != null && MarionetteAbilities.hasAbility(mob);
        int baseX = w / 2 - SLOT - 3;
        int baseY = h - VIGNETTE - SLOT - 8;
        drawTempSlot(graphics, baseX, baseY, "overlay.guimi_mod.marionette_control.melee", true);
        drawTempSlot(graphics, baseX + SLOT + 6, baseY, "overlay.guimi_mod.marionette_control.ability", hasAbility);
    }

    /** 单个角落的灵体之线：沿两轴伸出的细线 + 一条 45° 短斜线（阶梯点模拟）。 */
    private static void drawCornerThreads(GuiGraphics graphics, int x, int y, int dx, int dy) {
        // 水平 / 垂直细线
        graphics.hLine(Math.min(x, x + dx * THREAD_LEN), Math.max(x, x + dx * THREAD_LEN), y, THREAD_COLOR);
        graphics.vLine(x, Math.min(y, y + dy * THREAD_LEN), Math.max(y, y + dy * THREAD_LEN), THREAD_COLOR);
        // 斜向丝线：逐点绘制（细微、半透明）
        for (int i = 4; i < THREAD_LEN * 2 / 3; i += 2) {
            graphics.fill(x + dx * i, y + dy * i, x + dx * i + 1, y + dy * i + 1, THREAD_COLOR);
        }
    }

    /** 临时技能位：暗底方框 + 按键标签，不可用时灰显。 */
    private static void drawTempSlot(GuiGraphics graphics, int x, int y, String labelKey, boolean enabled) {
        Minecraft mc = Minecraft.getInstance();
        graphics.fill(x, y, x + SLOT, y + SLOT, enabled ? 0xAA1A0808 : 0xAA151515);
        int border = enabled ? 0xFF884444 : 0xFF444444;
        graphics.hLine(x, x + SLOT - 1, y, border);
        graphics.hLine(x, x + SLOT - 1, y + SLOT - 1, border);
        graphics.vLine(x, y, y + SLOT - 1, border);
        graphics.vLine(x + SLOT - 1, y, y + SLOT - 1, border);
        Component label = Component.translatable(labelKey);
        graphics.drawCenteredString(mc.font, label, x + SLOT / 2, y + SLOT + 2,
                enabled ? 0xFFDDDDDD : 0xFF777777);
    }

    /** 水平方向渐变填充（GuiGraphics 原生只有垂直渐变，这里用逐列插值实现）。 */
    private static void fillGradientHorizontal(GuiGraphics graphics, int x1, int y1, int x2, int y2,
                                               int colorFrom, int colorTo) {
        int width = x2 - x1;
        if (width <= 0) {
            return;
        }
        for (int i = 0; i < width; i++) {
            float t = (float) i / (float) width;
            int color = lerpColor(colorFrom, colorTo, t);
            graphics.fill(x1 + i, y1, x1 + i + 1, y2, color);
        }
    }

    private static int lerpColor(int from, int to, float t) {
        int a = (int) (((from >>> 24) & 0xFF) + t * (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)));
        int r = (int) (((from >>> 16) & 0xFF) + t * (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)));
        int g = (int) (((from >>> 8) & 0xFF) + t * (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)));
        int b = (int) ((from & 0xFF) + t * ((to & 0xFF) - (from & 0xFF)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
