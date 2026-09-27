package com.wan.gmmod.content.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 「封印侵蚀」——持有 / 穿戴封印物时承受的代价。
 * <p>
 * 每 2 秒造成一次魔法伤害，伤害随效果等级缩放：等级 0 时每 2 秒 0.5 点，
 * 封印的特性越强（序列等级越低）侵蚀越猛。移除封印物后效果自然消散。
 */
public class SealedCorruptionEffect extends MobEffect {

    public SealedCorruptionEffect() {
        // 有害效果，深紫近黑
        super(MobEffectCategory.HARMFUL, 0x2B0B3E);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(entity.damageSources().magic(), 0.5F * (amplifier + 1));
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // 每 40 刻（2 秒）跳一次侵蚀伤害
        return duration % 40 == 0;
    }
}
