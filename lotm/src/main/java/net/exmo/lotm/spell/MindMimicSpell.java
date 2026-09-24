package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.LotmSupport;
import net.exmo.lotm.effect.InfluenceBehavior;
import net.exmo.lotm.effect.LotmEffects;
import net.exmo.lotm.effect.MindMarks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/** Second sequence-8 active. The target treats your side as its own for 6 seconds. */
public final class MindMimicSpell extends AimedSpell {
    public static final double RANGE = 12.0;

    public MindMimicSpell() {
        super("mind_mimic", 40.0, 30, SchoolRegistry.ELDRITCH_RESOURCE, SoundEvents.EVOKER_CAST_SPELL,
                "spell.lotm.mind_mimic.guide");
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!(LotmTargeting.living(entity, RANGE) instanceof Mob)) {
            LotmSupport.tell(entity, "spell.lotm.need_mob");
            return false;
        }
        return true;
    }

    @Override
    protected void cast(ServerLevel level, int spellLevel, LivingEntity caster) {
        if (!(LotmTargeting.living(caster, RANGE) instanceof Mob mob)) return;
        MindMarks.mark(mob, caster);
        mob.addEffect(new MobEffectInstance(LotmEffects.MIND_MIMIC, 6 * 20, 0, false, true, true));
        InfluenceBehavior.tick(mob, MindMarks.Kind.MIMIC);
        LotmSupport.tell(caster, "spell.lotm.mind_mimic.applied");
    }
}
