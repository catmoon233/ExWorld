package com.wan.gmmod.client.gui;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.DistortionClientState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 扭曲模式 HUD：扭曲模式激活期间在屏幕中下方显示当前选中的扭曲类型、
 * 剩余窗口秒数与操作提示，帮助玩家确认目标选型。
 * <p>
 * 六种扭曲类型对应数字键 1~6；左键确认目标（实体 / 门），右键拖拽区域（隔绝房间）。
 */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public class DistortionHudOverlay {
    /** 扭曲类型显示名翻译键（与 DistortionCastPacket.T_* 顺序一致） */
    private static final String[] TYPE_KEYS = {
            "gui.guimi_mod.distortion.move_invert",
            "gui.guimi_mod.distortion.attack_redirect",
            "gui.guimi_mod.distortion.deflect",
            "gui.guimi_mod.distortion.seal_door",
            "gui.guimi_mod.distortion.isolate",
            "gui.guimi_mod.distortion.hijack",
    };

    private DistortionHudOverlay() {
    }

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(GuimiMod.id("distortion_hud"), DistortionHudOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!DistortionClientState.isModeActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || com.wan.gmmod.client.HudClientState.isHidden()) {
            return;
        }
        int w = graphics.guiWidth();
        int x = w / 2;
        int y = graphics.guiHeight() - 70;

        int selected = DistortionClientState.getSelectedType();
        String typeName = selected >= 0 && selected < TYPE_KEYS.length
                ? Component.translatable(TYPE_KEYS[selected]).getString()
                : "?";
        String line = Component.translatable("gui.guimi_mod.distortion.type_line",
                String.valueOf(selected + 1), typeName).getString();
        int lineW = mc.font.width(line);
        graphics.drawString(mc.font, line, x - lineW / 2, y, 0xFFE6CCFF, true);

        String hint = Component.translatable("gui.guimi_mod.distortion.hint").getString();
        int hintW = mc.font.width(hint);
        graphics.drawString(mc.font, hint, x - hintW / 2, y + 10, 0xFFCC99FF, true);

        String time = Component.translatable("gui.guimi_mod.distortion.remaining",
                String.valueOf(DistortionClientState.remainingTicks() / 20 + 1)).getString();
        int timeW = mc.font.width(time);
        graphics.drawString(mc.font, time, x - timeW / 2, y + 20, 0xFFAA77FF, true);
    }
}