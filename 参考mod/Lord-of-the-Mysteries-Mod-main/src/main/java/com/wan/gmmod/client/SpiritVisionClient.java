package com.wan.gmmod.client;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.vision.SpiritVision;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Client read of the synced spirit-vision flag, plus a short HUD notice. */
public final class SpiritVisionClient {
    private static String notice = "";
    private static long noticeUntil;

    private SpiritVisionClient() {}

    public static boolean allowed() {
        return SpiritVision.allowed(Minecraft.getInstance().player);
    }

    public static boolean isActive() {
        Player player = Minecraft.getInstance().player;
        return SpiritVision.allowed(player) && Boolean.TRUE.equals(player.getData(ModAttachments.SPIRIT_VISION));
    }

    public static void flash(String text) {
        notice = text == null ? "" : text;
        noticeUntil = System.currentTimeMillis() + 2000L;
    }

    public static String notice() {
        return System.currentTimeMillis() < noticeUntil ? notice : "";
    }
}