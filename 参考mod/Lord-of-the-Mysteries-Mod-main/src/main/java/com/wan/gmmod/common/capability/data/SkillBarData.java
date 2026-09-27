package com.wan.gmmod.common.capability.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能栏配置：固定 {@link #SIZE} 个槽位，每个槽位存储一个能力 ID 字符串（空串表示空槽）。
 * <p>
 * 不可变数据：所有修改都返回新实例，以便触发 NeoForge 附件的自动同步。
 *
 * @param slots 槽位列表，长度恒为 {@link #SIZE}
 */
public record SkillBarData(List<String> slots) {
    /** 技能栏槽位数量（3×5） */
    public static final int SIZE = 15;

    public static final Codec<SkillBarData> CODEC =
            Codec.STRING.listOf().xmap(SkillBarData::fromList, SkillBarData::slots);

    public static final StreamCodec<io.netty.buffer.ByteBuf, SkillBarData> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())
                    .map(SkillBarData::fromList, SkillBarData::slots);

    /** 全空技能栏。 */
    public static SkillBarData empty() {
        List<String> list = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            list.add("");
        }
        return new SkillBarData(list);
    }

    /** 从任意长度列表归一化为固定 {@link #SIZE} 长度。 */
    public static SkillBarData fromList(List<String> raw) {
        List<String> list = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            list.add(i < raw.size() && raw.get(i) != null ? raw.get(i) : "");
        }
        return new SkillBarData(list);
    }

    /** 取指定槽位的能力 ID，空槽返回 {@code null}。 */
    public ResourceLocation get(int slot) {
        if (slot < 0 || slot >= SIZE) {
            return null;
        }
        String s = slots.get(slot);
        return (s == null || s.isEmpty()) ? null : ResourceLocation.tryParse(s);
    }

    /** 返回将指定槽位设为 {@code id}（null 表示清空）后的新实例。 */
    public SkillBarData with(int slot, ResourceLocation id) {
        if (slot < 0 || slot >= SIZE) {
            return this;
        }
        List<String> copy = new ArrayList<>(slots);
        copy.set(slot, id == null ? "" : id.toString());
        return new SkillBarData(copy);
    }
}
