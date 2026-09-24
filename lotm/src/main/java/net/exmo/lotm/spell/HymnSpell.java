package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.LotmSupport;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** Hymn. Allies within 8 blocks gain Strength I and Speed I, and lose body discomfort. */
public final class HymnSpell extends AimedSpell {
    public static final double RADIUS = 8.0;
    private static final int DURATION = 20 * 20;

    public HymnSpell() {
        super("hymn", 30.0, 25, SchoolRegistry.HOLY_RESOURCE, SoundEvents.BEACON_ACTIVATE,
                "spell.lotm.hymn.guide");
    }

    @Override
    protected void cast(ServerLevel level, int spellLevel, LivingEntity caster) {
        AABB box = caster.getBoundingBox().inflate(RADIUS);
        int helped = 0;
        for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, box, living -> living.isAlive() && LotmSupport.friendly(caster, living))) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION, 0, false, true, true));
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION, 0, false, true, true));
            ally.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            ally.removeEffect(MobEffects.DIG_SLOWDOWN);
            ally.removeEffect(MobEffects.WEAKNESS);
            ally.removeEffect(MobEffects.CONFUSION);
            ally.removeEffect(MobEffects.BLINDNESS);
            ally.removeEffect(MobEffects.HUNGER);
            ally.removeEffect(MobEffects.DARKNESS);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, ally.getX(), ally.getY() + 1.0, ally.getZ(), 6, 0.3, 0.4, 0.3, 0.01);
            helped++;
        }
        LotmSupport.tell(caster, "spell.lotm.hymn.sung", helped);
    }
}
