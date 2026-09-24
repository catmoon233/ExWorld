package net.exmo.lotm.spell;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SpellTargets {
    private SpellTargets() {}

    public static LivingEntity living(LivingEntity caster, double range) {
        if (caster == null || range <= 0) return null;
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(range));
        AABB box = caster.getBoundingBox().expandTowards(caster.getLookAngle().scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(caster.level(), caster, eye, end, box,
                entity -> valid(caster, entity));
        if (hit != null && hit.getEntity() instanceof LivingEntity living) return living;
        return null;
    }

    public static Vec3 trapAnchor(LivingEntity caster, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(range));
        BlockHitResult hit = caster.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
        if (hit.getType() == HitResult.Type.BLOCK) {
            var above = hit.getBlockPos().relative(hit.getDirection());
            if (caster.level().getBlockState(above).canBeReplaced()) return Vec3.atBottomCenterOf(above);
            return hit.getLocation();
        }
        return caster.position();
    }

    private static boolean valid(LivingEntity caster, Entity entity) {
        return entity instanceof LivingEntity living
                && living != caster
                && living.isAlive()
                && !living.isSpectator()
                && living.isPickable();
    }
}
