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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Subtle guidance. Hostile targets scramble their attacks for 6s; neutral or friendly ones follow for 20s. */
public final class SubtleGuidanceSpell extends AimedSpell {
    public static final double RANGE = 12.0;

    public SubtleGuidanceSpell() {
        super("subtle_guidance", 30.0, 20, SchoolRegistry.ELDRITCH_RESOURCE, SoundEvents.ILLUSIONER_CAST_SPELL,
                "spell.lotm.subtle_guidance.guide");
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        LivingEntity target = LotmTargeting.living(entity, RANGE);
        if (!(target instanceof Mob)) {
            LotmSupport.tell(entity, target instanceof Player ? "spell.lotm.cannot_guide_player" : "spell.lotm.need_creature");
            return false;
        }
        return true;
    }

    @Override
    protected void cast(ServerLevel level, int spellLevel, LivingEntity caster) {
        if (!(LotmTargeting.living(caster, RANGE) instanceof Mob mob)) return;
        boolean hostile = LotmSupport.hostile(mob);
        MindMarks.Kind kind = hostile ? MindMarks.Kind.REDIRECT : MindMarks.Kind.FOLLOW;
        int duration = hostile ? 6 * 20 : 20 * 20;
        MindMarks.mark(mob, caster);
        mob.addEffect(new MobEffectInstance(hostile ? LotmEffects.GUIDANCE_REDIRECT : LotmEffects.GUIDANCE_FOLLOW,
                duration, 0, false, true, true));
        InfluenceBehavior.tick(mob, kind);
        LotmSupport.tell(caster, hostile ? "spell.lotm.subtle_guidance.hostile" : "spell.lotm.subtle_guidance.follow");
    }
}
