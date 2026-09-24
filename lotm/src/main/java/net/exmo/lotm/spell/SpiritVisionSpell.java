package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.effect.LotmEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/** Reveal nearby ores, traps, and creatures for 30 seconds. 10 mana, 20s cooldown. */
public final class SpiritVisionSpell extends InstantSpell {
    public static final int DURATION_TICKS = 30 * 20;
    public static final double RANGE = 16.0;

    public SpiritVisionSpell() {
        super("spirit_vision", SchoolRegistry.ELDRITCH_RESOURCE, 20.0, 10, SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        caster.addEffect(new MobEffectInstance(LotmEffects.SPIRIT_VISION, DURATION_TICKS, 0, false, true, true));
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ENCHANT, caster.getX(), caster.getEyeY(), caster.getZ(), 16, 0.45, 0.4, 0.45, 0.15);
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.spirit_vision.guide"));
    }
}
