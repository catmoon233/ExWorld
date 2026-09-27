package com.wan.gmmod.client.gui;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.registry.ModEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 失控状态的屏幕特效层：玩家拥有 {@code guimi_mod:losing_control} 效果期间，
 * 屏幕边缘出现扭曲噪点（随机闪烁的暗紫色 / 黑色噪块 + 边缘渐晕）。
 */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public class LosingControlOverlay {

    /** 屏幕边缘噪点带宽度（像素） */
    private static final int EDGE_BAND = 28;
    /** 每帧噪点块数量 */
    private static final int NOISE_COUNT = 120;

    private static final RandomSource RANDOM = RandomSource.create();

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(GuimiMod.id("losing_control_overlay"), LosingControlOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.hasEffect(ModEffects.LOSING_CONTROL)) return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        // 边缘渐晕：四周半透明暗紫色描边
        int vignette = 0x60250030;
        graphics.fill(0, 0, width, EDGE_BAND / 2, vignette);
        graphics.fill(0, height - EDGE_BAND / 2, width, height, vignette);
        graphics.fill(0, 0, EDGE_BAND / 2, height, vignette);
        graphics.fill(width - EDGE_BAND / 2, 0, width, height, vignette);

        // 扭曲噪点：仅出现在屏幕边缘噪点带内，每帧随机位置闪烁
        for (int i = 0; i < NOISE_COUNT; i++) {
            int size = 1 + RANDOM.nextInt(3);
            int x;
            int y;
            // 随机挑一条边缘带
            switch (RANDOM.nextInt(4)) {
                case 0 -> { x = RANDOM.nextInt(width); y = RANDOM.nextInt(EDGE_BAND); }
                case 1 -> { x = RANDOM.nextInt(width); y = height - 1 - RANDOM.nextInt(EDGE_BAND); }
                case 2 -> { x = RANDOM.nextInt(EDGE_BAND); y = RANDOM.nextInt(height); }
                default -> { x = width - 1 - RANDOM.nextInt(EDGE_BAND); y = RANDOM.nextInt(height); }
            }
            // 暗紫 / 黑白噪块随机混合
            int gray = RANDOM.nextInt(256);
            int color = RANDOM.nextBoolean()
                    ? (0xA0 << 24) | (gray << 16) | (gray << 8) | gray
                    : (0xA0 << 24) | 0x3B0A45;
            graphics.fill(x, y, x + size, y + size, color);
        }
    }
}
