package com.wan.gmmod.content.effects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 「黑焰灼烧」——被黑焰（女巫「操控黑焰」）命中的目标陷入的持续燃烧效果。
 * <p>
 * 每 0.5 秒造成 1.5 (+等级) 点魔法伤害，同时黑色火焰粒子与黑烟贴着目标
 * 身体燃起，模拟「黑色火焰附着在人物身上」的表现。源头见
 * {@link com.wan.gmmod.content.entities.BlackFlameEntity}。
 */
public class BlackFlameEffect extends MobEffect {
    public BlackFlameEffect() {
        super(MobEffectCategory.HARMFUL, 0x1a0b2e);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        double y = entity.getY() + entity.getBbHeight() * 0.85;
        if (level.isClientSide) {
            // 客户端渲染：黑色火焰粒子吸附在实体身上
            for (int i = 0; i < 6; i++) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        entity.getX() + (level.random.nextDouble() - 0.5) * 0.5,
                        y + (level.random.nextDouble() - 0.5) * 0.8,
                        entity.getZ() + (level.random.nextDouble() - 0.5) * 0.5,
                        0, 0.01, 0);
            }
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.SMOKE,
                        entity.getX() + (level.random.nextDouble() - 0.5) * 0.3,
                        y + level.random.nextDouble() * 0.6,
                        entity.getZ() + (level.random.nextDouble() - 0.5) * 0.3,
                        0, 0.04, 0);
            }
            return true;
        }
        entity.hurt(entity.damageSources().magic(), 1.5F + amplifier);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    entity.getX(), y, entity.getZ(), 6, 0.5, 0.8, 0.5, 0.01);
            server.sendParticles(ParticleTypes.SMOKE,
                    entity.getX(), y, entity.getZ(), 3, 0.3, 0.6, 0.3, 0.02);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // 每 10 刻（0.5 秒）一跳
        return duration % 10 == 0;
    }
}