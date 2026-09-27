package com.wan.gmmod.client;

import net.minecraft.core.BlockPos;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 方块高亮的客户端状态管理器（仅客户端）。
 * <p>
 * 收到 {@code HighlightBlocksPacket} 时通过 {@link #add(java.util.Collection, int)}
 * 登记一批需要描边的方块及其剩余持续 tick；每客户端 tick 调用 {@link #tick()}
 * 递减计时，到期后移除；渲染层通过 {@link #positions()} 获取当前需要描边的方块。
 */
public final class BlockHighlightClientState {
    /** 方块坐标 -> 剩余持续 tick */
    private static final Map<BlockPos, Integer> HIGHLIGHTS = new ConcurrentHashMap<>();

    private BlockHighlightClientState() {
    }

    /**
     * 登记一批需要高亮的方块。
     *
     * @param positions 方块坐标集合
     * @param ticks     持续 tick 数
     */
    public static void add(java.util.Collection<BlockPos> positions, int ticks) {
        for (BlockPos pos : positions) {
            // 使用不可变副本作为键，避免外部复用 MutableBlockPos 造成键污染
            HIGHLIGHTS.put(pos.immutable(), ticks);
        }
    }

    /**
     * 当前需要描边的方块集合（渲染层遍历使用）。
     */
    public static java.util.Set<BlockPos> positions() {
        return HIGHLIGHTS.keySet();
    }

    /**
     * 每客户端 tick 调用一次，递减所有计时，到期后移除。
     */
    public static void tick() {
        Iterator<Map.Entry<BlockPos, Integer>> it = HIGHLIGHTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }
}
