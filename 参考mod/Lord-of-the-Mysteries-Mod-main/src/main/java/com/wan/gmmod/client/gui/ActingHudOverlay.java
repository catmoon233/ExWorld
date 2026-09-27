package com.wan.gmmod.client.gui;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Top-left line: current sequence only. Acting stays on the sequence screen. */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public class ActingHudOverlay {
    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(GuimiMod.id("sequence_hud"), ActingHudOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || com.wan.gmmod.client.HudClientState.isHidden()) return;
        Sequences.Pathway pathway = Sequences.fromKey(player.getData(ModAttachments.PATHWAY));
        boolean employed = pathway != null && Sequences.employed(player.getData(ModAttachments.PATHWAY));
        String title = employed
                ? pathway.getDisplayName() + " · 序列" + player.getData(ModAttachments.SEQUENCE_LEVEL) + " " + pathway.getSequenceName(player.getData(ModAttachments.SEQUENCE_LEVEL))
                : "未就职";
        graphics.drawString(mc.font, Component.literal(title), 6, 6, employed ? 0xFFE8E8E8 : 0xFFAAAAAA, true);
    }
}