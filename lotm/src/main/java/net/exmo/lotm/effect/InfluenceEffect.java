package net.exmo.lotm.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class InfluenceEffect extends MobEffect {
    private final MindMarks.Kind kind;

    public InfluenceEffect(MindMarks.Kind kind, int color) {
        super(MobEffectCategory.NEUTRAL, color);
        this.kind = kind;
    }

    public MindMarks.Kind kind() {
        return kind;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        InfluenceBehavior.tick(entity, kind);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
