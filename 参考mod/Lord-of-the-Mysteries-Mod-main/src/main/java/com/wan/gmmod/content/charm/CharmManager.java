package com.wan.gmmod.content.charm;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 魅惑管理器（教唆者 / 欢愉魔女）。
 * <p>
 * 维护「被魅惑生物 UUID → 魅惑条目」表，由 {@code WitchAbilityEventSubscriber}
 * 的 EntityTickEvent 每刻驱动：
 * <ul>
 *   <li>{@link Type#CALM}：敌对生物中立化——持续清空其攻击目标；</li>
 *   <li>{@link Type#FOLLOW}：中立生物跟随施术者——周期性寻路到主人身边；</li>
 *   <li>{@link Type#MISDIRECT}：误导——持续把攻击目标锁定为指定受害者。</li>
 * </ul>
 * 条目不持久化（服务器重启即失效），符合"临时精神影响"的设定。
 */
public final class CharmManager {
    private CharmManager() {}

    public enum Type { CALM, FOLLOW, MISDIRECT }

    private record CharmEntry(Type type, UUID ownerId, UUID victimId, long endTick) {}

    /** 被魅惑生物 UUID → 条目。服务端单例表，跨维度共用。 */
    private static final Map<UUID, CharmEntry> ENTRIES = new ConcurrentHashMap<>();

    /** 中立化：敌对生物在持续时间内不再攻击任何目标。 */
    public static void calm(Mob mob, ServerPlayer owner, int durationTicks) {
        ENTRIES.put(mob.getUUID(), new CharmEntry(Type.CALM, owner.getUUID(), null,
                mob.level().getGameTime() + durationTicks));
        mob.setTarget(null);
        spawnHearts(mob);
    }

    /** 跟随：中立生物在持续时间内跟随施术者。 */
    public static void follow(Mob mob, ServerPlayer owner, int durationTicks) {
        ENTRIES.put(mob.getUUID(), new CharmEntry(Type.FOLLOW, owner.getUUID(), null,
                mob.level().getGameTime() + durationTicks));
        spawnHearts(mob);
    }

    /** 误导：让生物在持续时间内持续攻击指定受害者。 */
    public static void misdirect(Mob mob, ServerPlayer owner, LivingEntity victim, int durationTicks) {
        ENTRIES.put(mob.getUUID(), new CharmEntry(Type.MISDIRECT, owner.getUUID(), victim.getUUID(),
                mob.level().getGameTime() + durationTicks));
        mob.setTarget(victim);
        spawnAnger(mob);
    }

    /** 该生物当前是否处于任意魅惑状态。 */
    public static boolean isCharmed(Mob mob) {
        CharmEntry entry = ENTRIES.get(mob.getUUID());
        return entry != null && entry.endTick() > mob.level().getGameTime();
    }

    /**
     * 每刻驱动单个生物的魅惑行为（由 EntityTickEvent.Post 调用，仅服务端）。
     */
    public static void tickEntity(Mob mob) {
        CharmEntry entry = ENTRIES.get(mob.getUUID());
        if (entry == null) {
            return;
        }
        ServerLevel level = (ServerLevel) mob.level();
        if (level.getGameTime() >= entry.endTick() || !mob.isAlive()) {
            ENTRIES.remove(mob.getUUID());
            return;
        }
        switch (entry.type()) {
            case CALM -> mob.setTarget(null);
            case FOLLOW -> {
                mob.setTarget(null);
                if (mob.tickCount % 20 == 0
                        && level.getEntity(entry.ownerId()) instanceof LivingEntity owner
                        && mob.distanceTo(owner) > 3.0F) {
                    mob.getNavigation().moveTo(owner, 1.1);
                }
            }
            case MISDIRECT -> {
                if (level.getEntity(entry.victimId()) instanceof LivingEntity victim && victim.isAlive()) {
                    if (mob.getTarget() != victim) {
                        mob.setTarget(victim);
                    }
                } else {
                    ENTRIES.remove(mob.getUUID());
                }
            }
        }
        // 周期性爱心 / 怒气粒子提示魅惑状态
        if (mob.tickCount % 40 == 0) {
            if (entry.type() == Type.MISDIRECT) {
                spawnAnger(mob);
            } else {
                spawnHearts(mob);
            }
        }
    }

    private static void spawnHearts(Mob mob) {
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART,
                    mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(),
                    3, 0.3, 0.2, 0.3, 0.02);
        }
    }

    private static void spawnAnger(Mob mob) {
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(),
                    3, 0.3, 0.2, 0.3, 0.02);
        }
    }
}
