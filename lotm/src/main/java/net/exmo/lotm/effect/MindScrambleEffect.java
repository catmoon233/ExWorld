package net.exmo.lotm.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Marker plus mob-side stumble. Players are scrambled from their movement input on the client. */
public final class MindScrambleEffect extends MobEffect {
    public MindScrambleEffect() {
        super(MobEffectCategory.HARMFUL, 0x6A4A9A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide() || !(entity instanceof Mob mob) || !mob.isAlive()) return true;
        double angle = mob.getRandom().nextDouble() * Math.PI * 2.0;
        mob.getNavigation().stop();
        mob.getNavigation().moveTo(mob.getX() + Math.cos(angle) * 4.0, mob.getY(), mob.getZ() + Math.sin(angle) * 4.0, 1.1);
        mob.setTarget(null);
        mob.setDeltaMovement(Math.cos(angle) * 0.35, mob.getDeltaMovement().y, Math.sin(angle) * 0.35);
        mob.hasImpulse = true;
        return true;
    }
}
