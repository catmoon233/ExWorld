package com.wan.gmmod.common.capability.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * 冷却数据：能力 ID → 冷却结束时的游戏刻（{@code Level#getGameTime()}）。
 * <p>
 * 服务端权威写入，同步到客户端供技能栏 HUD 绘制冷却蒙版与倒计时。
 * 不可变：修改返回新实例以触发附件同步。
 *
 * @param endTicks 能力 ID 字符串 → 冷却结束游戏刻
 */
public record CooldownData(Map<String, Long> endTicks) {

    public static final Codec<CooldownData> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.LONG)
                    .xmap(m -> new CooldownData(new HashMap<>(m)), CooldownData::endTicks);

    public static final StreamCodec<io.netty.buffer.ByteBuf, CooldownData> STREAM_CODEC =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG)
                    .map(CooldownData::new, cd -> new HashMap<>(cd.endTicks()));

    public static CooldownData empty() {
        return new CooldownData(new HashMap<>());
    }

    /** 取能力冷却结束刻，无记录返回 0。 */
    public long getEnd(ResourceLocation id) {
        Long v = endTicks.get(id.toString());
        return v == null ? 0L : v;
    }

    /**
     * 返回记录 {@code id} 冷却结束刻为 {@code endTick} 后的新实例，
     * 同时清理所有早于 {@code now} 的过期记录，避免映射无限增长。
     */
    public CooldownData with(ResourceLocation id, long endTick, long now) {
        Map<String, Long> copy = new HashMap<>();
        for (Map.Entry<String, Long> e : endTicks.entrySet()) {
            if (e.getValue() > now) {
                copy.put(e.getKey(), e.getValue());
            }
        }
        copy.put(id.toString(), endTick);
        return new CooldownData(copy);
    }
}
