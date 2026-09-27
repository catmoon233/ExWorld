package com.wan.gmmod.client.gui;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.SpiritVisionClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Plain spirit-vision status. No chat line and no full-screen filter. */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public class SpiritVisionOverlay {
    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(GuimiMod.id("spirit_vision_status"), SpiritVisionOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int x = 8;
        int y = graphics.guiHeight() - 58;
        if (SpiritVisionClient.isActive()) {
            graphics.drawString(mc.font, Component.literal("灵视：开"), x, y, 0xFF8FD4FF, true);
        }
        String notice = SpiritVisionClient.notice();
        if (!notice.isEmpty()) {
            graphics.drawString(mc.font, Component.literal(notice), x, y - 12, 0xFFFFFFFF, true);
        }
    }
}