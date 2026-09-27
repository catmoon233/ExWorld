package com.wan.gmmod.content.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 「恶魔化」隐藏标记效果：纯粹声明玩家正处于深渊「序列 6 恶魔」的恶魔化
 * 持续生效期间，驱动客户端渲染巨大恶魔的 GeckoLib 全身模型（带翅膀与犄角）。
 * 由恶魔化能力激活时施加，随 buff 一同到期。
 */
public class DemonFormEffect extends MobEffect {
    public DemonFormEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7a1f1f);
    }
}
