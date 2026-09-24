package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** Heal the looked-at creature, or yourself, and strip one harmful effect. 25 mana, 15s cooldown. */
public final class HealingHandsSpell extends InstantSpell {
    public static final float HEAL = 8.0F;

    public HealingHandsSpell() {
        super("healing_hands", SchoolRegistry.NATURE_RESOURCE, 15.0, 25, SoundEvents.GENERIC_DRINK);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.GENERIC_DRINK);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        LivingEntity target = LotmTargeting.living(caster, 5.0);
        if (target == null) target = caster;
        target.heal(HEAL);
        MobEffectInstance harmful = null;
        for (MobEffectInstance instance : target.getActiveEffects()) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                harmful = instance;
                break;
            }
        }
        if (harmful != null) target.removeEffect(harmful.getEffect());
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.0, target.getZ(), 4, 0.3, 0.35, 0.3, 0.02);
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.healing_hands.guide"));
    }
}
