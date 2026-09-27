package com.wan.gmmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * 灵体之线的客户端状态（仅客户端）。
 * <p>
 * 由 {@link com.wan.gmmod.common.network.packet.SpiritThreadSyncPacket} 每约 10 刻更新一次，
 * 记录当前秘偶 / 操控目标的实体 network id 及挣扎状态；灵体之线视野下的
 * {@code SpiritThreadRenderer} 据此绘制玩家 ↔ 目标之间的线纹理。
 * 超过 {@link #STALE_TICKS} 未收到更新即视为失效并清空，避免线残留。
 */
public final class SpiritThreadClientState {
    /** 超过该客户端刻数未收到同步则视为失效 */
    private static final int STALE_TICKS = 30;

    private static int marionetteId = -1;
    private static int targetId = -1;
    private static boolean struggling;
    /** 最后一次收到同步包时的客户端游戏时间 */
    private static long lastUpdate = Long.MIN_VALUE;

    private SpiritThreadClientState() {
    }

    /** 收到同步包：更新秘偶 / 目标 id 与挣扎状态。 */
    public static void update(int marionette, int target, boolean struggle) {
        marionetteId = marionette;
        targetId = target;
        struggling = struggle;
        lastUpdate = clientTime();
    }

    /** 当前秘偶实体（客户端），不存在返回 null。 */
    public static Entity getMarionette() {
        return resolve(marionetteId);
    }

    /** 当前灵体之线操控中的目标实体（客户端），不存在返回 null。 */
    public static Entity getTarget() {
        return resolve(targetId);
    }

    /** 目标是否正在挣扎（线应变红并抖动）。 */
    public static boolean isStruggling() {
        return struggling;
    }

    /** 状态是否新鲜有效（最近收到过同步）。 */
    public static boolean isFresh() {
        return clientTime() - lastUpdate <= STALE_TICKS;
    }

    private static Entity resolve(int id) {
        if (id < 0 || !isFresh()) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.level != null ? mc.level.getEntity(id) : null;
        return entity != null && entity.isAlive() ? entity : null;
    }

    private static long clientTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0L;
    }
}
