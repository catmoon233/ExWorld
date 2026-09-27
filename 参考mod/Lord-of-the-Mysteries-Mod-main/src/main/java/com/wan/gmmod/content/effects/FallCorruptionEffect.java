package com.wan.gmmod.content.effects;

import com.wan.gmmod.common.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 「深渊化」——「堕落之物」范围攻击赋予的堕落状态。
 * <p>
 * 周期性地让受击者笼罩在黑暗中（深渊化倾向的黑暗视野），体表升起拖曳的黑色烟雾
 * （粘稠黑液化成的堕落黑雾）。深渊化期间，纸人 / 镜子 / 魔杖等替身类能力失效，
 * 受击者无法用替身脱身（对替身类能力的克制）。
 */
public class FallCorruptionEffect extends MobEffect {

    public FallCorruptionEffect() {
        // 有害效果，深渊般的漆黑
        super(MobEffectCategory.HARMFUL, 0x0A0710);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // 深渊化倾向的黑暗视野（每 20 刻续一次）
        entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, true));
        if (entity.level() instanceof ServerLevel level) {
            RandomSource random = level.getRandom();
            // 体表粘稠黑液上的堕落黑雾
            level.sendParticles(ParticleTypes.SMOKE,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                    3 + amplifier,
                    entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.4, entity.getBbWidth() * 0.4,
                    0.01);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration > 0 && duration % 20 == 0;
    }

    /** 该生物是否正处于堕落之物造成的「深渊化」中（替身类能力因此失效）。 */
    public static boolean isCorrupted(LivingEntity entity) {
        return entity != null && entity.hasEffect(ModEffects.FALL_CORRUPTION);
    }
}