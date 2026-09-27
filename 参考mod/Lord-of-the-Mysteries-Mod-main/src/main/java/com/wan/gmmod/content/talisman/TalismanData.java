package com.wan.gmmod.content.talisman;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 符咒的核心数据：祈求对象 + 符咒类型。
 * <p>
 * 作为 {@link net.minecraft.core.component.DataComponentType} 挂在符咒物品的
 * {@link net.minecraft.world.item.ItemStack} 上，合成时由祭台写入。
 * <ul>
 *   <li>{@code deity}：祈求对象英文标识（如 {@code "sun"} 太阳、{@code "night"} 黑夜、
 *       {@code "tyrant"} 暴君），决定符咒的效果池归属；</li>
 *   <li>{@code type}：符咒类型（{@code "purification"} / {@code "requiem"} /
 *       {@code "electric"}），投掷激发时据此触发对应效果。</li>
 * </ul>
 */
public record TalismanData(String deity, String type) {
    public static final Codec<TalismanData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("deity").forGetter(TalismanData::deity),
            Codec.STRING.fieldOf("type").forGetter(TalismanData::type)
    ).apply(inst, TalismanData::new));

    public static final StreamCodec<ByteBuf, TalismanData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, TalismanData::deity,
                    ByteBufCodecs.STRING_UTF8, TalismanData::type,
                    TalismanData::new
            );
}