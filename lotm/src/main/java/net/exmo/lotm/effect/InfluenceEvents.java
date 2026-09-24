package net.exmo.lotm.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** Stops a charmed mob from snapping back onto the caster, and drops the caster link when the effect ends. */
public final class InfluenceEvents {
    private InfluenceEvents() {}

    @SubscribeEvent
    public static void retarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        MindMarks.Kind kind = active(mob);
        if (kind == null) return;
        if (InfluenceBehavior.forbids(mob, event.getNewAboutToBeSetTarget(), kind)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void removed(MobEffectEvent.Remove event) {
        clear(event.getEntity(), event.getEffectInstance());
    }

    @SubscribeEvent
    public static void expired(MobEffectEvent.Expired event) {
        clear(event.getEntity(), event.getEffectInstance());
    }

    private static void clear(LivingEntity entity, MobEffectInstance instance) {
        if (instance != null && instance.getEffect().value() instanceof InfluenceEffect) {
            MindMarks.clear(entity);
        }
    }

    private static MindMarks.Kind active(Mob mob) {
        if (mob.hasEffect(LotmEffects.GUIDANCE_REDIRECT)) return MindMarks.Kind.REDIRECT;
        if (mob.hasEffect(LotmEffects.GUIDANCE_FOLLOW)) return MindMarks.Kind.FOLLOW;
        if (mob.hasEffect(LotmEffects.MIND_MIMIC)) return MindMarks.Kind.MIMIC;
        return null;
    }
}
