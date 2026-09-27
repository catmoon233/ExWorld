package com.wan.gmmod.content.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 「狼人化」隐藏标记效果：纯粹声明玩家正处于被缚者「序列 7 狼人」的狼人化
 * 持续生效期间，驱动客户端渲染半狼人的 GeckoLib 叠加模型（狼头 / 利爪 / 尾巴，
 * 双腿保持人形）。由狼人化能力激活时施加，随 buff 一同到期。
 */
public class WerewolfFormEffect extends MobEffect {
    public WerewolfFormEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x6b5848);
    }
}
