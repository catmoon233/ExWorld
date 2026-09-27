package com.wan.gmmod.content.ancient;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 古代神秘物品的变体数据：同一通用物品类 {@link AncientArtifactItem} 之下，
 * 以 variant 字符串区分具体实例（破碎的圣像手指 / 疯人院入院记录 / 焦灼的圣袍边角 / 血染的六便士）。
 * <p>
 * 属于「有少许神秘力量的古代物品」通用系统：后续新增实例只需扩展 variant，
 * 无需新的物品类。组件随物品堆栈持久化。
 *
 * @param variant 实例变体标识（如 {@code "broken_icon_finger"}）
 */
public record AncientArtifactData(String variant) {

    public static final Codec<AncientArtifactData> CODEC =
            Codec.STRING.xmap(AncientArtifactData::new, AncientArtifactData::variant);

    public static final StreamCodec<io.netty.buffer.ByteBuf, AncientArtifactData> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(AncientArtifactData::new, AncientArtifactData::variant);
}
