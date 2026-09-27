package com.wan.gmmod.client.gui;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 直觉预警（猎人「直觉预警」/ 纵火家「危险直觉」）的屏幕特效层：
 * 服务端检测到背后（升级后 360°）有威胁靠近时，
 * {@code DANGER_SENSE} 附件同步为 true，屏幕边缘泛起随心跳脉动的微红渐晕。
 */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public class DangerSenseOverlay {

    /** 边缘红晕带宽度（像素） */
    private static final int EDGE_BAND = 22;

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(GuimiMod.id("danger_sense_overlay"), DangerSenseOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.getData(ModAttachments.DANGER_SENSE)) return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        // 随心跳脉动的透明度（周期约 1 秒），微红不遮挡视野
        float pulse = Mth.sin((player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(false)) * 0.3F);
        int alpha = (int) (0x28 + 0x20 * (pulse * 0.5F + 0.5F));
        int vignette = (alpha << 24) | 0xFF1010;

        // 双层边缘渐晕：外圈较深、内圈较浅
        graphics.fill(0, 0, width, EDGE_BAND / 2, vignette);
        graphics.fill(0, height - EDGE_BAND / 2, width, height, vignette);
        graphics.fill(0, 0, EDGE_BAND / 2, height, vignette);
        graphics.fill(width - EDGE_BAND / 2, 0, width, height, vignette);

        int inner = ((alpha / 2) << 24) | 0xFF1010;
        graphics.fill(0, EDGE_BAND / 2, width, EDGE_BAND, inner);
        graphics.fill(0, height - EDGE_BAND, width, height - EDGE_BAND / 2, inner);
        graphics.fill(0, EDGE_BAND / 2, EDGE_BAND, height - EDGE_BAND / 2, inner);
        graphics.fill(width - EDGE_BAND, EDGE_BAND / 2, width - EDGE_BAND / 2, height - EDGE_BAND / 2, inner);
    }
}
