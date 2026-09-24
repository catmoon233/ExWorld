package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.LotmSupport;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** A flash in front of the caster. The cone is blinded; undead also take a small hit. */
public final class FlashSpell extends InstantSpell {
    public FlashSpell() {
        super("flash", 15.0, 15, SchoolRegistry.EVOCATION_RESOURCE);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    @Override
    protected void cast(ServerLevel level, int spellLevel, LivingEntity caster) {
        Vec3 look = caster.getLookAngle();
        Vec3 origin = caster.getEyePosition();
        AABB box = caster.getBoundingBox().expandTowards(look.scale(6.0)).inflate(1.5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                living -> living != caster && living.isAlive() && !living.isSpectator())) {
            Vec3 to = target.getEyePosition().subtract(origin);
            double distance = to.length();
            if (distance > 6.0 || distance < 1.0E-4) continue;
            if (to.normalize().dot(look) < 0.35) continue;
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, true, true));
            if (LotmSupport.undead(target)) target.hurt(getDamageSource(caster), 3.0F);
        }
        Vec3 flash = origin.add(look.scale(1.5));
        level.sendParticles(ParticleTypes.FLASH, flash.x, flash.y, flash.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, flash.x, flash.y, flash.z, 12, 0.4, 0.3, 0.4, 0.02);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.flash.guide"));
    }
}
