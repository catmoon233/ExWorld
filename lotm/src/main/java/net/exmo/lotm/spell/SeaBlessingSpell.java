package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.effect.LotmEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/** Water breathing, night vision, and unslowed water movement for 60 seconds. 15 mana, 30s cooldown. */
public final class SeaBlessingSpell extends InstantSpell {
    public static final int DURATION_TICKS = 60 * 20;

    public SeaBlessingSpell() {
        super("sea_blessing", SchoolRegistry.NATURE_RESOURCE, 30.0, 15, SoundEvents.CONDUIT_ACTIVATE);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        caster.addEffect(new MobEffectInstance(LotmEffects.SEA_BLESSING, DURATION_TICKS, 0, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, DURATION_TICKS, 0, false, false, false));
        caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION_TICKS, 0, false, false, false));
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.BUBBLE, caster.getX(), caster.getY() + 1.0, caster.getZ(), 18, 0.4, 0.5, 0.4, 0.02);
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.sea_blessing.guide"));
    }
}
