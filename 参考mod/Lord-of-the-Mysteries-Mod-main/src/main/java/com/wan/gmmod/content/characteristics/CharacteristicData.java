package com.wan.gmmod.content.characteristics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 非凡特性的核心数据：途径标识 + 序列等级。
 * <p>
 * 作为 {@link net.minecraft.core.component.DataComponentType} 挂在特性物品的 {@link net.minecraft.world.item.ItemStack} 上，
 * 所有途径的特性共用同一种物品（{@code characteristic}），仅靠本数据区分。
 * 途径以 {@link com.wan.gmmod.content.sequences.Sequences.Pathway#getKey()} 存储，
 * 等级沿用序列约定（9 = 最弱入门，0 = 最强真神）。
 *
 * @param pathway 途径英文标识（如 {@code "fool"}）
 * @param level   序列等级（0 ~ 9）
 */
public record CharacteristicData(String pathway, int level) {
    public static final Codec<CharacteristicData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("pathway").forGetter(CharacteristicData::pathway),
            Codec.INT.fieldOf("level").forGetter(CharacteristicData::level)
    ).apply(inst, CharacteristicData::new));

    public static final StreamCodec<io.netty.buffer.ByteBuf, CharacteristicData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CharacteristicData::pathway,
                    ByteBufCodecs.VAR_INT, CharacteristicData::level,
                    CharacteristicData::new
            );
}
