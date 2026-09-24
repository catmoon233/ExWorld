package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.effect.LotmEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

/** See through invisibility and hidden forms for 15 seconds. 20 mana, 25s cooldown. */
public final class TrueSightSpell extends InstantSpell {
    public static final int DURATION_TICKS = 15 * 20;
    public static final double RANGE = 24.0;

    public TrueSightSpell() {
        super("true_sight", SchoolRegistry.ELDRITCH_RESOURCE, 25.0, 20, SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        caster.addEffect(new MobEffectInstance(LotmEffects.TRUE_SIGHT, DURATION_TICKS, 0, false, true, true));
        if (!(level instanceof ServerLevel server)) return;
        server.sendParticles(ParticleTypes.ENCHANT, caster.getX(), caster.getEyeY(), caster.getZ(), 16, 0.4, 0.5, 0.4, 0.2);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.true_sight.guide"));
    }
}
