package io.redspace.irons_artifice.entity.ai;

import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class GunCombatMoveControl {
    protected static final UniformInt PATH_DELAY = TimeUtil.rangeOfSeconds(1, 2);

    public enum PositionMode {
        FLEE,
        BACKPEDAL,
        ORBIT,
        CLOSE_GAP,
        CHARGE,
        PLANT
    }

    protected int pathDelay;
    protected int strafingTime = -1;
    protected boolean strafingClockwise;
    protected boolean strafingBackwards;

    public PositionMode selectKiting(double distSqr, boolean hasLos, AiGunRange bands) {
        if (!hasLos || distSqr > bands.idealMaxSqr()) {
            return PositionMode.CLOSE_GAP;
        }
        if (distSqr < bands.panicSqr()) {
            return PositionMode.FLEE;
        }
        if (distSqr < bands.idealMinSqr()) {
            return PositionMode.BACKPEDAL;
        }
        return PositionMode.ORBIT;
    }

    public void tick(Mob mob, LivingEntity target, AiGunRange bands, PositionMode mode, double chargeSpeed) {
        double distSqr = mob.distanceToSqr(target);
        switch (mode) {
            case FLEE -> flee(mob, target, bands);
            case BACKPEDAL -> backpedal(mob, target, distSqr, bands);
            case ORBIT -> orbit(mob, target, distSqr, bands);
            case CLOSE_GAP -> closeGap(mob, target);
            case CHARGE -> charge(mob, target, chargeSpeed);
            case PLANT -> plant(mob, target);
        }
    }

    public void reset() {
        pathDelay = 0;
        strafingTime = -1;
    }

    protected void flee(Mob mob, LivingEntity target, AiGunRange bands) {
        pathDelay--;
        if (pathDelay <= 0) {
            Vec3 away = mob.position().subtract(target.position());
            if (away.lengthSqr() < 1.0E-4) {
                away = new Vec3(mob.getRandom().nextFloat() - 0.5, 0, mob.getRandom().nextFloat() - 0.5);
            }
            away = away.normalize();
            Vec3 dest = mob.position().add(away.scale(bands.idealRangeMin()));
            if (!mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0)) {
                mob.getMoveControl().strafe(-1.0F, strafingClockwise ? 0.5F : -0.5F);
            }
            pathDelay = PATH_DELAY.sample(mob.getRandom());
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    protected void backpedal(Mob mob, LivingEntity target, double distSqr, AiGunRange bands) {
        mob.getNavigation().stop();
        updateStrafeDirection(mob);
        float lateral = strafingClockwise ? 0.5F : -0.5F;
        mob.getMoveControl().strafe(-0.7F, lateral);
        mob.lookAt(target, 30.0F, 30.0F);
    }

    protected void orbit(Mob mob, LivingEntity target, double distSqr, AiGunRange bands) {
        mob.getNavigation().stop();
        updateStrafeDirection(mob);

        boolean isGoingTheOtherWayThanLateral = true;
        if (distSqr > bands.idealMaxSqr() * 0.8 * 0.8) {
            strafingBackwards = false;
        } else if (distSqr < bands.idealMinSqr() * 1.2 * 1.2) {
            strafingBackwards = true;
        } else {
            isGoingTheOtherWayThanLateral = false;
        }

        float forward = !isGoingTheOtherWayThanLateral ? 0 : (strafingBackwards ? -0.5F : 0.5F);
        float lateral = strafingClockwise ? 0.5F : -0.5F;
        mob.getMoveControl().strafe(forward, lateral);
        mob.lookAt(target, 30.0F, 30.0F);
    }

    protected void closeGap(Mob mob, LivingEntity target) {
        pathDelay--;
        if (pathDelay <= 0) {
            mob.getNavigation().moveTo(target, 1.0);
            pathDelay = PATH_DELAY.sample(mob.getRandom());
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        strafingTime = -1;
    }

    protected void charge(Mob mob, LivingEntity target, double chargeSpeed) {
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        mob.setYRot(mob.yHeadRot);
        mob.getNavigation().stop();
        mob.getMoveControl().strafe((float) chargeSpeed, 0);
        strafingTime = -1;
    }

    protected void plant(Mob mob, LivingEntity target) {
        mob.getNavigation().stop();
        mob.getMoveControl().strafe(0, 0);
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        mob.setYRot(mob.yHeadRot);
    }

    protected void updateStrafeDirection(Mob mob) {
        if (strafingTime < 0) {
            strafingTime = 0;
        }
        strafingTime++;
        if (strafingTime >= 20) {
            if (mob.getRandom().nextFloat() < 0.3F) {
                strafingClockwise = !strafingClockwise;
            }
            if (mob.getRandom().nextFloat() < 0.3F) {
                strafingBackwards = !strafingBackwards;
            }
            strafingTime = 0;
        }
    }
}
