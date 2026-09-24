package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

/** Speed III for 20 seconds. 30 mana, 45s cooldown. */
public final class SwiftSpell extends InstantSpell {
    public SwiftSpell() {
        super("swift", SchoolRegistry.ELDRITCH_RESOURCE, 45.0, 30, SoundEvents.PLAYER_ATTACK_SWEEP);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.PLAYER_ATTACK_SWEEP);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 20, 2, false, true, true));
        if (!(level instanceof ServerLevel server)) return;
        server.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + 0.2, caster.getZ(), 10, 0.3, 0.1, 0.3, 0.02);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.swift.guide"));
    }
}
