package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.LotmSupport;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Second sequence-8 active. Cleanse a friendly who is carrying harmful effects, including yourself. */
public final class ExorcismSpell extends AimedSpell {
    public static final double RANGE = 8.0;

    public ExorcismSpell() {
        super("exorcism", 20.0, 25, SchoolRegistry.HOLY_RESOURCE, SoundEvents.TOTEM_USE, "spell.lotm.exorcism.guide");
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (target(entity) == null) {
            LotmSupport.tell(entity, "spell.lotm.no_debuff");
            return false;
        }
        return true;
    }

    @Override
    protected void cast(ServerLevel level, int spellLevel, LivingEntity caster) {
        LivingEntity target = target(caster);
        if (target == null) return;
        for (Holder<MobEffect> effect : LotmSupport.harmfulEffects(target)) {
            target.removeEffect(effect);
        }
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 8 * 20, 0, false, true, true));
        level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        LotmSupport.tell(caster, "spell.lotm.exorcism.cleansed", target.getName());
    }

    private static LivingEntity target(LivingEntity caster) {
        LivingEntity looked = LotmTargeting.living(caster, RANGE);
        if (looked != null && LotmSupport.friendly(caster, looked) && LotmSupport.hasHarmful(looked)) return looked;
        if (LotmSupport.hasHarmful(caster)) return caster;
        return null;
    }
}
