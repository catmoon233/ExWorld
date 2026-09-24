package net.exmo.lotm.spell;

import net.exmo.lotm.LotmSupport;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Server-side follow-through for tracking arrows, taunt, and whispered confusion. */
public final class LotmSpellRuntime {
    public static final double TAUNT_RANGE = 8.0;
    private static final ResourceLocation SHRED = ResourceLocation.fromNamespaceAndPath("lotm", "taunt_shred");
    private static final List<Homing> HOMING = new ArrayList<>();
    private static final List<Bleed> BLEEDS = new ArrayList<>();
    private static final List<Timed> SHREDS = new ArrayList<>();
    private static final List<Timed> SCRAMBLES = new ArrayList<>();

    private LotmSpellRuntime() {}

    public static void launchArrow(ServerLevel level, LivingEntity caster) {
        Arrow arrow = new Arrow(EntityType.ARROW, level);
        arrow.setOwner(caster);
        arrow.setNoGravity(true);
        arrow.setCritArrow(true);
        arrow.setBaseDamage(4.0);
        Vec3 look = caster.getLookAngle();
        Vec3 pos = caster.getEyePosition().add(look.scale(0.8));
        arrow.setPos(pos.x, pos.y, pos.z);
        arrow.shoot(look.x, look.y, look.z, 2.2F, 0.4F);
        LivingEntity locked = LotmSupport.lookedAt(caster, 24.0);
        if (locked != null && LotmSupport.allied(caster, locked)) locked = null;
        if (locked == null) locked = nearestAhead(caster, look, 24.0);
        HOMING.add(new Homing(arrow.getUUID(), locked == null ? null : locked.getUUID(), caster.getUUID(), level.getGameTime() + 60));
        level.addFreshEntity(arrow);
    }

    public static boolean hasTauntTarget(LivingEntity caster) {
        if (caster == null) return false;
        AABB box = caster.getBoundingBox().inflate(TAUNT_RANGE);
        for (Mob mob : caster.level().getEntitiesOfClass(Mob.class, box, mob -> tauntable(caster, mob))) {
            if (mob.distanceToSqr(caster) <= TAUNT_RANGE * TAUNT_RANGE) return true;
        }
        return false;
    }

    public static void roar(ServerLevel level, LivingEntity caster) {
        AABB box = caster.getBoundingBox().inflate(TAUNT_RANGE);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, mob -> tauntable(caster, mob))) {
            if (mob.distanceToSqr(caster) > TAUNT_RANGE * TAUNT_RANGE) continue;
            mob.setTarget(caster);
            mob.setLastHurtByMob(caster);
            mob.setAggressive(true);
            shred(mob, level.getGameTime() + 80);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.1F, 0.9F);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, caster.getX(), caster.getY() + 1.2, caster.getZ(), 12, 0.6, 0.4, 0.6, 0.02);
    }

    public static void whisper(ServerLevel level, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        replace(SCRAMBLES, target.getUUID(), level.getGameTime() + 100);
        level.playSound(null, target.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.7F, 0.6F);
    }

    @SubscribeEvent
    public static void steer(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        Iterator<Homing> shots = HOMING.iterator();
        while (shots.hasNext()) {
            Homing shot = shots.next();
            Entity entity = level.getEntity(shot.arrowId);
            if (!(entity instanceof AbstractArrow arrow) || !arrow.isAlive() || now > shot.expireAt || arrow.onGround()) {
                if (entity instanceof AbstractArrow stuck) stuck.setNoGravity(false);
                if (entity == null && now <= shot.expireAt) continue;
                shots.remove();
                continue;
            }
            LivingEntity target = resolve(level, shot.targetId);
            if (target == null || !target.isAlive() || target.distanceToSqr(arrow) > 18 * 18) {
                target = nearest(level, arrow, shot.ownerId, 12.0);
                shot.targetId = target == null ? null : target.getUUID();
            }
            if (target == null) continue;
            Vec3 to = target.getBoundingBox().getCenter().subtract(arrow.position());
            if (to.lengthSqr() < 0.36) continue;
            double speed = Math.max(1.45, arrow.getDeltaMovement().length());
            Vec3 blended = arrow.getDeltaMovement().scale(0.25).add(to.normalize().scale(speed * 0.75));
            if (blended.lengthSqr() > 1.0E-6) arrow.setDeltaMovement(blended.normalize().scale(speed));
            arrow.hasImpulse = true;
        }
    }

    @SubscribeEvent
    public static void follow(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        tickBleeds(level, now);
        tickShreds(level, now);
        tickScrambles(level, now);
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || arrow.level().isClientSide()) return;
        Homing shot = null;
        Iterator<Homing> shots = HOMING.iterator();
        while (shots.hasNext()) {
            Homing next = shots.next();
            if (next.arrowId.equals(arrow.getUUID())) {
                shot = next;
                shots.remove();
                break;
            }
        }
        if (shot == null) return;
        arrow.setNoGravity(false);
        if (event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living
                && living.isAlive()) {
            BLEEDS.add(new Bleed(living.getUUID(), shot.ownerId, 5, arrow.level().getGameTime() + 20));
        }
    }

    private static void tickBleeds(ServerLevel level, long now) {
        Iterator<Bleed> bleeds = BLEEDS.iterator();
        while (bleeds.hasNext()) {
            Bleed bleed = bleeds.next();
            if (now < bleed.nextAt) continue;
            Entity entity = level.getEntity(bleed.targetId);
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                bleeds.remove();
                continue;
            }
            LivingEntity owner = resolve(level, bleed.ownerId);
            var source = owner == null
                    ? target.damageSources().magic()
                    : LotmSpells.TRACKING_ARROW.get().getDamageSource(owner).setIFrames(0);
            target.hurt(source, 1.0F);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.0, target.getZ(), 4, 0.2, 0.3, 0.2, 0.02);
            bleed.pulsesLeft--;
            bleed.nextAt = now + 20;
            if (bleed.pulsesLeft <= 0) bleeds.remove();
        }
    }

    private static void tickShreds(ServerLevel level, long now) {
        Iterator<Timed> shreds = SHREDS.iterator();
        while (shreds.hasNext()) {
            Timed shred = shreds.next();
            if (now < shred.expireAt) continue;
            Entity entity = level.getEntity(shred.id);
            if (entity instanceof LivingEntity living) clearShred(living);
            shreds.remove();
        }
    }

    private static void tickScrambles(ServerLevel level, long now) {
        Iterator<Timed> scrambles = SCRAMBLES.iterator();
        while (scrambles.hasNext()) {
            Timed scramble = scrambles.next();
            if (now >= scramble.expireAt) {
                scrambles.remove();
                continue;
            }
            if (now < scramble.nextPulse) continue;
            Entity entity = level.getEntity(scramble.id);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                scrambles.remove();
                continue;
            }
            double angle = living.getRandom().nextDouble() * Math.PI * 2.0;
            double speed = living instanceof net.minecraft.world.entity.player.Player ? 0.28 : 0.42;
            living.setDeltaMovement(Math.cos(angle) * speed, living.getDeltaMovement().y, Math.sin(angle) * speed);
            living.hasImpulse = true;
            if (living instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.getNavigation().moveTo(living.getX() + Math.cos(angle) * 4.0, living.getY(), living.getZ() + Math.sin(angle) * 4.0, 1.1);
            }
            scramble.nextPulse = now + 10;
        }
    }

    private static boolean tauntable(LivingEntity caster, Mob mob) {
        return mob.isAlive() && mob != caster && !LotmSupport.allied(caster, mob) && LotmSupport.hostile(mob);
    }

    private static void shred(LivingEntity target, long expireAt) {
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.addOrUpdateTransientModifier(new AttributeModifier(SHRED, -4.0, AttributeModifier.Operation.ADD_VALUE));
        }
        replace(SHREDS, target.getUUID(), expireAt);
    }

    private static void clearShred(LivingEntity target) {
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.removeModifier(SHRED);
    }

    private static void replace(List<Timed> list, UUID id, long expireAt) {
        list.removeIf(entry -> entry.id.equals(id));
        list.add(new Timed(id, expireAt, 0));
    }

    private static LivingEntity resolve(ServerLevel level, UUID id) {
        if (id == null) return null;
        Entity entity = level.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static LivingEntity nearestAhead(LivingEntity caster, Vec3 look, double range) {
        LivingEntity best = null;
        double bestDistance = range * range;
        AABB box = caster.getBoundingBox().inflate(range);
        for (LivingEntity living : caster.level().getEntitiesOfClass(LivingEntity.class, box,
                candidate -> candidate != caster && candidate.isAlive() && !LotmSupport.allied(caster, candidate))) {
            Vec3 to = living.getEyePosition().subtract(caster.getEyePosition());
            double distance = to.lengthSqr();
            if (distance > bestDistance || distance < 1.0E-4) continue;
            if (to.normalize().dot(look) < 0.35) continue;
            bestDistance = distance;
            best = living;
        }
        return best;
    }

    private static LivingEntity nearest(ServerLevel level, Entity from, UUID ownerId, double range) {
        LivingEntity owner = resolve(level, ownerId);
        LivingEntity best = null;
        double bestDistance = range * range;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(range),
                candidate -> candidate.isAlive() && candidate != from && (owner == null || !LotmSupport.allied(owner, candidate)))) {
            double distance = living.distanceToSqr(from);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = living;
            }
        }
        return best;
    }

    private static final class Homing {
        final UUID arrowId;
        UUID targetId;
        final UUID ownerId;
        final long expireAt;

        Homing(UUID arrowId, UUID targetId, UUID ownerId, long expireAt) {
            this.arrowId = arrowId;
            this.targetId = targetId;
            this.ownerId = ownerId;
            this.expireAt = expireAt;
        }
    }

    private static final class Bleed {
        final UUID targetId;
        final UUID ownerId;
        int pulsesLeft;
        long nextAt;

        Bleed(UUID targetId, UUID ownerId, int pulsesLeft, long nextAt) {
            this.targetId = targetId;
            this.ownerId = ownerId;
            this.pulsesLeft = pulsesLeft;
            this.nextAt = nextAt;
        }
    }

    private static final class Timed {
        final UUID id;
        final long expireAt;
        long nextPulse;

        Timed(UUID id, long expireAt, long nextPulse) {
            this.id = id;
            this.expireAt = expireAt;
            this.nextPulse = nextPulse;
        }
    }
}
