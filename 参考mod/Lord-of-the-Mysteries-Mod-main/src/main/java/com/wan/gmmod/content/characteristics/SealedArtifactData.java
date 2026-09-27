package com.wan.gmmod.content.characteristics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 封印物的核心数据：来源特性（途径 + 序列等级）与封印时所用的基底物品。
 * <p>
 * 作为 {@link net.minecraft.core.component.DataComponentType} 挂在封印物物品
 * （{@code sealed_artifact}）的 {@link net.minecraft.world.item.ItemStack} 上，
 * 所有途径、所有等级、任意基底物品共用同一种物品，仅靠本数据区分。
 * 途径以 {@link com.wan.gmmod.content.sequences.Sequences.Pathway#getKey()} 存储，
 * 基底物品以注册表 ID 字符串存储（如 {@code "minecraft:iron_sword"}）。
 *
 * @param pathway  来源特性的途径英文标识（如 {@code "fool"}）
 * @param level    来源特性的序列等级（0 ~ 9）
 * @param baseItem 封印时消耗的基底物品注册表 ID
 */
public record SealedArtifactData(String pathway, int level, String baseItem) {
    public static final Codec<SealedArtifactData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("pathway").forGetter(SealedArtifactData::pathway),
            Codec.INT.fieldOf("level").forGetter(SealedArtifactData::level),
            Codec.STRING.fieldOf("base_item").forGetter(SealedArtifactData::baseItem)
    ).apply(inst, SealedArtifactData::new));

    public static final StreamCodec<io.netty.buffer.ByteBuf, SealedArtifactData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SealedArtifactData::pathway,
                    ByteBufCodecs.VAR_INT, SealedArtifactData::level,
                    ByteBufCodecs.STRING_UTF8, SealedArtifactData::baseItem,
                    SealedArtifactData::new
            );
}
