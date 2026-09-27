package com.wan.gmmod.content.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 「黎明加持」隐藏标记效果：纯粹声明玩家正处于「黎明命甲」「晨曦之剑」的
 * 持续生效期间，驱动客户端渲染对应的 GeckoLib 模型（身上铠甲 / 手中光剑），
 * 并供 Quest / 事件逻辑查询。由技能激活时施加，随 buff 一同到期。
 */
public class DawnMarkEffect extends MobEffect {
    public DawnMarkEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xffd27a);
    }
}