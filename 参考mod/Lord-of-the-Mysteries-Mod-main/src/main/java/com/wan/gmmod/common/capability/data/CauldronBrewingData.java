package com.wan.gmmod.common.capability.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * 炼药锅状态数据（世界级附件）：记录哪些坐标的原版水锅已被注入「净水」而成为纯水锅，
 * 以及其被木棍搅拌的累计次数。
 * <p>
 * 键为 {@link BlockPos#asLong()} 的字符串形式；值为搅拌次数。存在于映射中即表示该坐标为纯水锅。
 * 服务端权威，随世界存档持久化，无需同步到客户端。
 *
 * @param stirs 打包坐标字符串 → 搅拌次数
 */
public record CauldronBrewingData(Map<String, Integer> stirs) {

    public static final Codec<CauldronBrewingData> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .xmap(m -> new CauldronBrewingData(new HashMap<>(m)), CauldronBrewingData::stirs);

    public static CauldronBrewingData empty() {
        return new CauldronBrewingData(new HashMap<>());
    }

    private static String key(BlockPos pos) {
        return Long.toString(pos.asLong());
    }

    /** 该坐标是否为纯水锅。 */
    public boolean isPurified(BlockPos pos) {
        return stirs.containsKey(key(pos));
    }

    /** 将该坐标标记为纯水锅（搅拌次数归零）。 */
    public void markPurified(BlockPos pos) {
        stirs.put(key(pos), 0);
    }

    /** 取该坐标的搅拌次数，未记录返回 0。 */
    public int getStir(BlockPos pos) {
        return stirs.getOrDefault(key(pos), 0);
    }

    /** 搅拌一次并返回累计次数。 */
    public int addStir(BlockPos pos) {
        int next = getStir(pos) + 1;
        stirs.put(key(pos), next);
        return next;
    }

    /** 清除该坐标的纯水锅状态（合成完成或失败重置）。 */
    public void clear(BlockPos pos) {
        stirs.remove(key(pos));
    }
}
