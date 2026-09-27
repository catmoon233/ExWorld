package com.wan.gmmod.client;

import net.minecraft.client.Minecraft;

/**
 * 冥想的客户端状态：由 {@link com.wan.gmmod.common.network.packet.MeditationSyncPacket} 驱动。
 * <p>
 * 冥想期间：
 * - 记录进入冥想时的视角并每 tick 强制还原（视角固定）
 * - 配合 MovementInputUpdateEvent 清零移动输入（无法移动）
 */
public class MeditationClientState {

    private static boolean meditating = false;
    private static float fixedYaw;
    private static float fixedPitch;

    /** 设置冥想状态；开始冥想时记录当前视角作为固定视角。 */
    public static void setMeditating(boolean value) {
        Minecraft mc = Minecraft.getInstance();
        if (value && mc.player != null) {
            fixedYaw = mc.player.getYRot();
            fixedPitch = mc.player.getXRot();
        }
        meditating = value;
    }

    public static boolean isMeditating() {
        return meditating;
    }

    /** 每客户端 tick 调用：冥想期间强制固定视角。 */
    public static void tick() {
        if (!meditating) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            meditating = false;
            return;
        }
        mc.player.setYRot(fixedYaw);
        mc.player.setXRot(fixedPitch);
        mc.player.yRotO = fixedYaw;
        mc.player.xRotO = fixedPitch;
        mc.player.setYHeadRot(fixedYaw);
    }
}
