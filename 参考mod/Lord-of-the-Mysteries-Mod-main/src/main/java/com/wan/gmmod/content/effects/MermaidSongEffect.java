package com.wan.gmmod.content.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 「美人鱼的歌声」隐藏 Buff：纯标记效果，无任何 tick 行为。
 * <p>
 * 由美人鱼实体（或高序列替代能力）周期性施加，施加时统一走
 * {@link com.wan.gmmod.content.ritual.MermaidSongManager}，
 * 以隐藏形式（ambient、不可见、无图标）添加。真假 Buff 外观完全相同，
 * 真伪只记录在服务端附件中，服用魔药时才悄悄判定。
 */
public class MermaidSongEffect extends MobEffect {
    public MermaidSongEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x35c9b0);
    }
}
