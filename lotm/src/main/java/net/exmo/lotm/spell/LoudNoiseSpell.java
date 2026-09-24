package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.ApprenticePassives;
import net.exmo.lotm.LotmSupport;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** A crack of noise that turns nearby hostiles toward the point for a few seconds. */
public final class LoudNoiseSpell extends InstantSpell {
    public LoudNoiseSpell() {
        super("loud_noise", 25.0, 20, SchoolRegistry.EVOCATION_RESOURCE);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.LIGHTNING_BOLT_THUNDER);
    }

    @Override
    protected void cast(ServerLevel level, int spellLevel, LivingEntity caster) {
        Vec3 point = LotmSupport.lookedAtPoint(caster, 10.0);
        level.playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4F, 0.7F);
        level.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        ApprenticePassives.attract(level, point, 80);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.loud_noise.guide"));
    }
}
