package com.wan.gmmod.content.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 「切割流血」——散射纸牌命中后的持续伤害效果。
 * <p>
 * 每秒造成 0.5 点魔法伤害（默认持续 3 秒，共 1.5 点），
 * 凸显小丑飞牌的致命优雅。见 {@link com.wan.gmmod.content.entities.FlyingCardEntity}。
 */
public class BleedingEffect extends MobEffect {

    public BleedingEffect() {
        // 有害效果，暗红色
        super(MobEffectCategory.HARMFUL, 0x8A0303);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(entity.damageSources().magic(), 0.5F);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // 每 20 刻（1 秒）跳一次伤害
        return duration % 20 == 0;
    }
}
