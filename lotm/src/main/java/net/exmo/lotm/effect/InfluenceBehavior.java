package net.exmo.lotm.effect;

import net.exmo.lotm.LotmSupport;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Keeps a charmed mob pointed at the right target for the effect duration. */
public final class InfluenceBehavior {
    private InfluenceBehavior() {}

    public static void tick(LivingEntity entity, MindMarks.Kind kind) {
        if (!(entity instanceof Mob mob) || mob.level().isClientSide() || !mob.isAlive()) return;
        LivingEntity caster = MindMarks.caster(mob);
        switch (kind) {
            case FOLLOW -> follow(mob, caster);
            case REDIRECT -> redirect(mob, caster);
            case MIMIC -> mimic(mob, caster);
        }
    }

    public static boolean forbids(Mob mob, LivingEntity next, MindMarks.Kind kind) {
        if (next == null) return false;
        LivingEntity caster = MindMarks.caster(mob);
        if (caster == null) return false;
        if (kind == MindMarks.Kind.FOLLOW) return true;
        return next == caster || next.isAlliedTo(caster);
    }

    private static void follow(Mob mob, LivingEntity caster) {
        mob.setTarget(null);
        if (caster == null || caster.level() != mob.level()) return;
        double distance = mob.distanceTo(caster);
        if (distance <= 2.4) {
            mob.getNavigation().stop();
            return;
        }
        mob.getNavigation().moveTo(caster, 1.15);
        if (distance > 3.0) {
            Vec3 pull = caster.position().subtract(mob.position()).normalize().scale(0.05);
            double lift = mob instanceof FlyingMob && caster.getY() - mob.getY() > 1.0 ? 0.04 : 0.0;
            mob.setDeltaMovement(mob.getDeltaMovement().add(pull.x, lift, pull.z));
        }
    }

    private static void redirect(Mob mob, LivingEntity caster) {
        if (caster != null && mob.getTarget() == caster) mob.setTarget(null);
        if (mob.tickCount % 20 != 0 && mob.getTarget() != null && mob.getTarget().isAlive()) return;
        mob.setTarget(randomOther(mob, caster, 8.0));
    }

    private static void mimic(Mob mob, LivingEntity caster) {
        LivingEntity current = mob.getTarget();
        if (current == caster || (caster != null && current != null && current.isAlliedTo(caster))) {
            mob.setTarget(null);
        }
        if (mob.tickCount % 10 != 0 && mob.getTarget() != null && mob.getTarget().isAlive()) return;
        LivingEntity next = preferOtherMonster(mob, caster, 10.0);
        if (next != null) mob.setTarget(next);
    }

    private static LivingEntity randomOther(Mob mob, LivingEntity caster, double range) {
        List<LivingEntity> choices = nearby(mob, caster, range, candidate -> true);
        if (choices.isEmpty()) return null;
        return choices.get(mob.getRandom().nextInt(choices.size()));
    }

    private static LivingEntity preferOtherMonster(Mob mob, LivingEntity caster, double range) {
        List<LivingEntity> others = nearby(mob, caster, range, candidate -> candidate instanceof Monster && candidate.getType() != mob.getType());
        if (!others.isEmpty()) return nearest(mob, others);
        List<LivingEntity> monsters = nearby(mob, caster, range, candidate -> candidate instanceof Monster);
        if (!monsters.isEmpty()) return nearest(mob, monsters);
        List<LivingEntity> any = nearby(mob, caster, range, candidate -> caster == null || !LotmSupport.allied(caster, candidate));
        return any.isEmpty() ? null : nearest(mob, any);
    }

    private static List<LivingEntity> nearby(Mob mob, LivingEntity caster, double range, Predicate<LivingEntity> extra) {
        AABB box = mob.getBoundingBox().inflate(range);
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity candidate : mob.level().getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (candidate == mob || candidate == caster) continue;
            if (caster != null && candidate.isAlliedTo(caster)) continue;
            if (!extra.test(candidate)) continue;
            found.add(candidate);
        }
        return found;
    }

    private static LivingEntity nearest(Mob mob, List<LivingEntity> choices) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : choices) {
            double distance = mob.distanceToSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }
}
