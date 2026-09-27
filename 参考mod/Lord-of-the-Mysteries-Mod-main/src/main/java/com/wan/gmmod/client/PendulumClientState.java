package com.wan.gmmod.client;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 灵摆使用状态的客户端管理器（仅客户端）。
 * <p>
 * 记录当前「正在使用灵摆」的玩家及其剩余 tick。收到
 * {@code PendulumUsePacket} 时调用 {@link #startUsing(UUID, int)} 登记，
 * 每客户端 tick 调用 {@link #tick()} 递减计时；
 * {@code PlayerModelMixin} 在渲染时通过 {@link #isUsing(UUID)}
 * 判断是否需要把该玩家的右臂旋转到胸前。
 * <p>
 * 使用玩家 UUID 作为键，保证多人环境下每个玩家的手臂各自独立驱动。
 */
public final class PendulumClientState {
    /** 玩家 UUID -> 剩余持续 tick */
    private static final Map<UUID, Integer> USING = new ConcurrentHashMap<>();

    private PendulumClientState() {
    }

    /**
     * 登记某玩家开始使用灵摆。
     *
     * @param playerId 玩家 UUID
     * @param ticks    持续 tick 数
     */
    public static void startUsing(UUID playerId, int ticks) {
        USING.put(playerId, ticks);
    }

    /**
     * 判断指定玩家当前是否正在使用灵摆。
     */
    public static boolean isUsing(UUID playerId) {
        return USING.containsKey(playerId);
    }

    /**
     * 每客户端 tick 调用一次，递减所有计时，到期后移除。
     */
    public static void tick() {
        Iterator<Map.Entry<UUID, Integer>> it = USING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }
}
