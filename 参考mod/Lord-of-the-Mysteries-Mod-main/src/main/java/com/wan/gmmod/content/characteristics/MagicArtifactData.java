package com.wan.gmmod.content.characteristics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 神奇物品（封印物）的核心数据：来源特性（途径 + 序列等级）、随机性变体与植入基底物品。
 * <p>
 * 神奇物品是「将非凡特性植入基础物品」后形成的超凡物品——拥有正面加强、负面作用，
 * 且效果具有随机性。作为 {@link net.minecraft.core.component.DataComponentType} 挂在
 * 神奇物品的 {@link net.minecraft.world.item.ItemStack} 上，所有途径、所有等级、
 * 任意基底物品共用同一种物品，仅靠本数据区分。
 * <p>
 * 变体 {@link #variant()} 在物品生成时随机掷定（0 = 标准版，1~3 = 三种随机性变体），
 * 决定能力的具体参数浮动。途径以 {@link com.wan.gmmod.content.sequences.Sequences.Pathway#getKey()}
 * 存储，基底物品以注册表 ID 字符串存储（如 {@code "minecraft:iron_sword"}）。
 *
 * @param pathway  来源特性的途径英文标识（如 {@code "door"}）
 * @param level    来源特性的序列等级（0 ~ 9）
 * @param variant  随机性变体（0 = 标准，1 ~ 3 = 三种变体）
 * @param baseItem 植入时消耗的基底物品注册表 ID
 */
public record MagicArtifactData(String pathway, int level, int variant, String baseItem) {
    public static final Codec<MagicArtifactData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("pathway").forGetter(MagicArtifactData::pathway),
            Codec.INT.fieldOf("level").forGetter(MagicArtifactData::level),
            Codec.INT.fieldOf("variant").forGetter(MagicArtifactData::variant),
            Codec.STRING.fieldOf("base_item").forGetter(MagicArtifactData::baseItem)
    ).apply(inst, MagicArtifactData::new));

    public static final StreamCodec<io.netty.buffer.ByteBuf, MagicArtifactData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, MagicArtifactData::pathway,
                    ByteBufCodecs.VAR_INT, MagicArtifactData::level,
                    ByteBufCodecs.VAR_INT, MagicArtifactData::variant,
                    ByteBufCodecs.STRING_UTF8, MagicArtifactData::baseItem,
                    MagicArtifactData::new
            );
}
