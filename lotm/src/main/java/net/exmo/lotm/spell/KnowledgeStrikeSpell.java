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

/** Next melee hit is a critical and ignores 40% of armor reduction. 15 mana, 15s cooldown. */
public final class KnowledgeStrikeSpell extends InstantSpell {
    public static final int DURATION_TICKS = 30 * 20;
    public static final float CRIT = 1.5F;
    public static final float ARMOR_KEPT = 0.6F;

    public KnowledgeStrikeSpell() {
        super("knowledge_strike", SchoolRegistry.ELDRITCH_RESOURCE, 15.0, 15, SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        caster.addEffect(new MobEffectInstance(LotmEffects.KNOWLEDGE_STRIKE, DURATION_TICKS, 0, false, true, true));
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ENCHANTED_HIT, caster.getX(), caster.getEyeY(), caster.getZ(), 12, 0.35, 0.3, 0.35, 0.05);
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.knowledge_strike.guide"));
    }
}
