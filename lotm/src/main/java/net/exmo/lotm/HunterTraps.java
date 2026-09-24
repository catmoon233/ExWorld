package net.exmo.lotm;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Hidden hunter traps and the short root they apply. Server-side only. */
public final class HunterTraps {
    private static final int MAX_PER_OWNER = 3;
    private static final int LIFE_TICKS = 45 * 20;
    public static final int ROOT_TICKS = 60;
    private static final double RADIUS = 1.15;

    private static final List<Trap> TRAPS = new ArrayList<>();
    private static final List<Root> ROOTS = new ArrayList<>();

    private HunterTraps() {}

    public static void place(ServerLevel level, ServerPlayer owner, Vec3 pos, float damage, ResourceLocation spellId) {
        long now = level.getGameTime();
        TRAPS.removeIf(trap -> trap.owner.equals(owner.getUUID()) && now >= until(level, trap));
        int owned = 0;
        for (Trap trap : TRAPS) if (trap.owner.equals(owner.getUUID())) owned++;
        while (owned >= MAX_PER_OWNER) {
            Trap oldest = null;
            for (Trap trap : TRAPS) {
                if (!trap.owner.equals(owner.getUUID())) continue;
                if (oldest == null || trap.until < oldest.until) oldest = trap;
            }
            if (oldest == null) break;
            TRAPS.remove(oldest);
            owned--;
        }
        TRAPS.add(new Trap(level.dimension(), owner.getUUID(), pos, damage, spellId, now + LIFE_TICKS));
        level.sendParticles(owner, ParticleTypes.ASH, false, pos.x, pos.y + 0.2, pos.z, 8, 0.2, 0.05, 0.2, 0.01);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<Trap> traps = TRAPS.iterator();
        while (traps.hasNext()) {
            Trap trap = traps.next();
            if (!trap.dimension.equals(level.dimension())) continue;
            if (now >= trap.until) {
                traps.remove();
                continue;
            }
            if (now % 15L == 0L) hint(level, trap);
            LivingEntity victim = victim(level, trap);
            if (victim == null) continue;
            spring(level, trap, victim);
            traps.remove();
        }
        Iterator<Root> roots = ROOTS.iterator();
        while (roots.hasNext()) {
            Root root = roots.next();
            if (!root.dimension.equals(level.dimension())) continue;
            if (now >= root.until) {
                roots.remove();
                continue;
            }
            if (level.getEntity(root.entity) instanceof LivingEntity living && living.isAlive()) hold(living);
        }
    }

    private static long until(ServerLevel level, Trap trap) {
        return trap.dimension.equals(level.dimension()) ? trap.until : Long.MAX_VALUE;
    }

    private static void hint(ServerLevel level, Trap trap) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(trap.owner);
        if (owner == null) return;
        level.sendParticles(owner, ParticleTypes.ASH, false, trap.pos.x, trap.pos.y + 0.12, trap.pos.z, 1, 0.04, 0.02, 0.04, 0.0);
    }

    private static LivingEntity victim(ServerLevel level, Trap trap) {
        AABB box = new AABB(trap.pos, trap.pos).inflate(RADIUS, 1.0, RADIUS);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(trap.owner);
        List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, box, living -> canTrigger(owner, trap, living));
        return hits.isEmpty() ? null : hits.get(0);
    }

    private static boolean canTrigger(ServerPlayer owner, Trap trap, LivingEntity living) {
        if (!living.isAlive() || living.isSpectator() || living.getUUID().equals(trap.owner)) return false;
        if (living instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        return owner == null || !living.isAlliedTo(owner);
    }

    private static void spring(ServerLevel level, Trap trap, LivingEntity victim) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(trap.owner);
        AbstractSpell spell = SpellRegistry.getSpell(trap.spellId);
        DamageSource source = owner != null && spell != null && spell != SpellRegistry.none()
                ? spell.getDamageSource(owner)
                : level.damageSources().magic();
        victim.hurt(source, trap.damage);
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ROOT_TICKS, 5));
        ROOTS.add(new Root(level.dimension(), victim.getUUID(), level.getGameTime() + ROOT_TICKS));
        hold(victim);
        level.sendParticles(ParticleTypes.SMOKE, trap.pos.x, trap.pos.y + 0.3, trap.pos.z, 12, 0.2, 0.1, 0.2, 0.01);
        level.playSound(null, trap.pos.x, trap.pos.y, trap.pos.z, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.PLAYERS, 0.7F, 0.8F);
    }

    private static void hold(LivingEntity living) {
        Vec3 motion = living.getDeltaMovement();
        living.setDeltaMovement(0.0, Math.min(0.0, motion.y), 0.0);
        living.setSprinting(false);
        living.hasImpulse = true;
        if (living instanceof Mob mob) mob.getNavigation().stop();
    }

    private record Trap(ResourceKey<Level> dimension, UUID owner, Vec3 pos, float damage, ResourceLocation spellId, long until) {}

    private record Root(ResourceKey<Level> dimension, UUID entity, long until) {}
}
