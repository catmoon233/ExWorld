package com.wan.gmmod.common.capability.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * 当前变形状态数据。
 * <p>
 * {@link #sourceType} 决定渲染方式：
 * <ul>
 *     <li>{@code NONE}：未变形，渲染原始玩家；</li>
 *     <li>{@code PLAYER}：变形为其他玩家皮肤（记忆库，预留）；</li>
 *     <li>{@code MOB}：变形为人形怪物，{@link #targetId} 为怪物实体类型 ID（如 {@code minecraft:zombie}）。</li>
 * </ul>
 * 不可变：所有修改都返回新实例，以触发 NeoForge 附件的自动同步。
 *
 * @param sourceType 变形来源类型字符串（NONE / PLAYER / MOB）
 * @param targetId   目标标识：MOB 时为实体类型 ID，PLAYER 时为玩家名，NONE 时为空串
 */
public record DisguiseData(String sourceType, String targetId) {
    public static final String NONE = "NONE";
    public static final String PLAYER = "PLAYER";
    public static final String MOB = "MOB";

    public static final Codec<DisguiseData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("source_type").forGetter(DisguiseData::sourceType),
            Codec.STRING.fieldOf("target_id").forGetter(DisguiseData::targetId)
    ).apply(inst, DisguiseData::new));

    public static final StreamCodec<io.netty.buffer.ByteBuf, DisguiseData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, DisguiseData::sourceType,
                    ByteBufCodecs.STRING_UTF8, DisguiseData::targetId,
                    DisguiseData::new
            );

    /** 未变形状态。 */
    public static DisguiseData none() {
        return new DisguiseData(NONE, "");
    }

    /** 变形为指定人形怪物。 */
    public static DisguiseData ofMob(ResourceLocation mobId) {
        return new DisguiseData(MOB, mobId == null ? "" : mobId.toString());
    }

    /** 是否处于变形状态（PLAYER 或 MOB）。 */
    public boolean isDisguised() {
        return !NONE.equals(sourceType) && targetId != null && !targetId.isEmpty();
    }

    /** 是否变形为怪物。 */
    public boolean isMob() {
        return MOB.equals(sourceType) && targetId != null && !targetId.isEmpty();
    }

    /** 变形目标怪物类型 ID（非 MOB 时返回 {@code null}）。 */
    public ResourceLocation mobId() {
        return isMob() ? ResourceLocation.tryParse(targetId) : null;
    }
}
