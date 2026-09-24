package net.exmo.lotm.graffiti;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Short-lived graffiti marks. Enemies standing on one are slowed and glow. */
public final class GraffitiTraps {
    public static final int LIFETIME_TICKS = 25 * 20;
    private static final int MAX_PER_OWNER = 4;
    private static final Map<ResourceKey<Level>, List<Trap>> TRAPS = new HashMap<>();

    private GraffitiTraps() {}

    public static void place(ServerLevel level, BlockPos pos, UUID owner) {
        List<Trap> traps = TRAPS.computeIfAbsent(level.dimension(), key -> new ArrayList<>());
        int owned = 0;
        for (Trap trap : traps) {
            if (owner.equals(trap.owner)) owned++;
        }
        if (owned >= MAX_PER_OWNER) {
            Iterator<Trap> iterator = traps.iterator();
            while (iterator.hasNext() && owned >= MAX_PER_OWNER) {
                if (owner.equals(iterator.next().owner)) {
                    iterator.remove();
                    owned--;
                }
            }
        }
        traps.add(new Trap(pos.immutable(), owner, level.getGameTime() + LIFETIME_TICKS));
        level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 0.5F, 1.3F);
        pulseParticles(level, pos);
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        List<Trap> traps = TRAPS.get(level.dimension());
        if (traps == null || traps.isEmpty()) return;
        if (level.getGameTime() % 5L != 0L) return;
        long now = level.getGameTime();
        traps.removeIf(trap -> trap.expireAt <= now);
        for (Trap trap : traps) affect(level, trap);
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) TRAPS.remove(level.dimension());
    }

    private static void affect(ServerLevel level, Trap trap) {
        if (!level.isLoaded(trap.pos)) return;
        pulseParticles(level, trap.pos);
        AABB box = new AABB(trap.pos).inflate(0.15, 0.2, 0.15);
        Player owner = level.getPlayerByUUID(trap.owner);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, living -> enemy(living, trap.owner, owner))) {
            boolean fresh = !living.hasEffect(MobEffects.GLOWING);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false, true));
            if (fresh) {
                level.playSound(null, trap.pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.35F, 1.5F);
            }
        }
    }

    private static boolean enemy(LivingEntity living, UUID ownerId, Player owner) {
        if (!living.isAlive() || living.isSpectator() || living.getUUID().equals(ownerId)) return false;
        if (living instanceof Animal) return false;
        if (living instanceof Player player) return owner == null || !player.isAlliedTo(owner);
        return living instanceof Enemy;
    }

    private static void pulseParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.12, pos.getZ() + 0.5, 5, 0.28, 0.04, 0.28, 0.0);
    }

    private record Trap(BlockPos pos, UUID owner, long expireAt) {}
}
