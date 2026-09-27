package com.wan.gmmod.content.war;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 燃烧之墙管理器——纵火家（战争之红途径 · 序列 7）「燃烧之墙」的服务端逻辑。
 * <p>
 * 维护所有存活火墙：半径 3 米的火环，持续 8 秒；穿越火环（位于环带内）的
 * 生物每秒最多受到一次 5 点火焰伤害并燃烧。由 WarAbilityEventSubscriber
 * 在每个 ServerLevel tick 驱动；同时供「火焰跃迁」查询/引爆。
 */
public final class FireWallManager {
    /** 火环半径：3 米。 */
    public static final double RADIUS = 3.0;
    /** 环带判定宽度（半径 ± 0.6 米）。 */
    private static final double BAND = 0.6;
    /** 穿越伤害与同一实体的伤害节流间隔。 */
    private static final float DAMAGE = 5.0F;
    private static final int HURT_INTERVAL = 20;

    private static final List<Wall> WALLS = new ArrayList<>();

    private FireWallManager() {}

    /** 一圈火墙的运行时状态。 */
    public static final class Wall {
        final ResourceKey<Level> dimension;
        final Vec3 center;
        final UUID ownerId;
        final long endTime;
        final Map<UUID, Long> lastHurt = new HashMap<>();

        Wall(ResourceKey<Level> dimension, Vec3 center, UUID ownerId, long endTime) {
            this.dimension = dimension;
            this.center = center;
            this.ownerId = ownerId;
            this.endTime = endTime;
        }

        public Vec3 center() {
            return center;
        }

        public UUID ownerId() {
            return ownerId;
        }
    }

    /** 在指定位置升起一圈火墙。 */
    public static void addWall(ServerLevel level, Vec3 center, UUID ownerId, int durationTicks) {
        WALLS.add(new Wall(level.dimension(), center, ownerId, level.getGameTime() + durationTicks));
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** 每个 ServerLevel tick 调用：粒子展示 + 穿越伤害 + 到期清理。 */
    public static void tick(ServerLevel level) {
        if (WALLS.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Wall> it = WALLS.iterator();
        while (it.hasNext()) {
            Wall wall = it.next();
            if (!wall.dimension.equals(level.dimension())) {
                continue;
            }
            if (now >= wall.endTime) {
                it.remove();
                continue;
            }
            // 环形火焰粒子（每 2 刻一圈，16 个采样点）
            if (now % 2 == 0) {
                for (int i = 0; i < 16; i++) {
                    double angle = (now % 360 / 8.0) + i * Math.PI * 2 / 16;
                    double px = wall.center.x + Math.cos(angle) * RADIUS;
                    double pz = wall.center.z + Math.sin(angle) * RADIUS;
                    level.sendParticles(ParticleTypes.FLAME, px, wall.center.y + 0.2, pz,
                            3, 0.1, 0.6, 0.1, 0.01);
                }
            }
            // 穿越者（位于环带内）受火焰伤害 + 燃烧，同一实体 1 秒节流
            AABB box = new AABB(wall.center, wall.center).inflate(RADIUS + BAND, 2.5, RADIUS + BAND);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && !e.getUUID().equals(wall.ownerId))) {
                double dist = Math.sqrt(victim.distanceToSqr(wall.center.x, victim.getY(), wall.center.z));
                if (Math.abs(dist - RADIUS) > BAND) {
                    continue;
                }
                long last = wall.lastHurt.getOrDefault(victim.getUUID(), 0L);
                if (now - last < HURT_INTERVAL) {
                    continue;
                }
                wall.lastHurt.put(victim.getUUID(), now);
                LivingEntity owner = level.getPlayerByUUID(wall.ownerId);
                victim.hurt(owner != null
                        ? level.damageSources().indirectMagic(owner, owner)
                        : level.damageSources().inFire(), DAMAGE);
                victim.igniteForSeconds(4);
            }
        }
    }

    /** 查询指定玩家最近的火墙（供火焰跃迁），无则返回 null。 */
    public static Wall nearestWall(ServerLevel level, Vec3 from, UUID ownerId, double range) {
        Wall best = null;
        double bestDist = range * range;
        for (Wall wall : WALLS) {
            if (!wall.dimension.equals(level.dimension()) || !wall.ownerId.equals(ownerId)) {
                continue;
            }
            double d = wall.center.distanceToSqr(from);
            if (d < bestDist) {
                bestDist = d;
                best = wall;
            }
        }
        return best;
    }

    /** 移除并引爆一圈火墙（火焰跃迁抵达后调用）。 */
    public static void removeWall(Wall wall) {
        WALLS.remove(wall);
    }
}
