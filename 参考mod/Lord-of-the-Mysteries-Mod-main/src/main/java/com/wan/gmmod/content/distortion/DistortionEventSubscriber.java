package com.wan.gmmod.content.distortion;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.DistortionZoneData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Comparator;
import java.util.UUID;

/**
 * 扭曲能力的事件驱动机制：
 * <ul>
 *   <li><b>弹射物偏转</b>：开启偏转窗口的施法者周围 10 格内的弹射物每 tick 被
 *   施加向上 / 反向偏移，最多持续到窗口结束；</li>
 *   <li><b>移动反向</b>：带 {@code move_invert} 标记的目标移动向量被反向并受到
 *   反向推力；</li>
 *   <li><b>攻击转移</b>：带 {@code attack_redirect} 标记的攻击者攻击施法者时，
 *   本次攻击被取消，攻击目标被强制改为附近最近的其他生物；</li>
 *   <li><b>封闭屏障</b>：屏障内实体试图穿出边界时被回推，左键攻击边界方块削减耐久；</li>
 *   <li><b>隔绝房间</b>：非授权玩家踏入即被强制退出，房间内外攻击互相阻断；</li>
 *   <li><b>到期清理</b>：实体扭曲标记 / 扭曲区域 / 弹射物窗口到点后移除，
 *   门被再次打开时封闭屏障提前消失。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public final class DistortionEventSubscriber {

    /** 区域维护周期（tick） */
    private static final int ZONE_INTERVAL = 10;
    /** 弹射物偏转施加的向上增量（每 tick） */
    private static final double DEFLECT_UPWARD = 0.35;
    /** 弹射物偏转施加的横向扰动 */
    private static final double DEFLECT_SWERVE = 0.10;
    /** 攻击转移：取消后给攻击者的新目标搜索半径 */
    private static final double REDIRECT_SEARCH_RADIUS = 10.0;
    /** 封闭屏障回推 / 伤害系数 */
    private static final double SEAL_PUSH_BACK = 0.8;

    private DistortionEventSubscriber() {
    }

    // =====================================================================
    // 实体 tick：弹射物偏转 / 移动反向 / 标记清理
    // =====================================================================

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) entity.level();

        // --- 弹射物偏转（B）：对范围内所有弹射物施加向上 / 横向偏移 ---
        if (entity instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            for (ServerPlayer sp : level.players()) {
                if (owner == sp || !DistortionManager.isDeflecting(sp)
                        || sp.distanceToSqr(projectile) > DistortionManager.DEFLECT_RANGE
                                * DistortionManager.DEFLECT_RANGE) {
                    continue;
                }
                Vec3 motion = projectile.getDeltaMovement();
                projectile.setDeltaMovement(
                        motion.x + level.random.nextDouble() * DistortionManager.DEFLECT_SWERVE
                                - DistortionManager.DEFLECT_SWERVE / 2,
                        motion.y + DistortionManager.DEFLECT_UPWARD,
                        motion.z + level.random.nextDouble() * DistortionManager.DEFLECT_SWERVE
                                - DistortionManager.DEFLECT_SWERVE / 2);
                projectile.hasImpulse = true;
                level.sendParticles(ParticleTypes.ENCHANT,
                        projectile.getX(), projectile.getY(), projectile.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
                break;
            }
            return;
        }

        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        // --- 移动反向（D）---（仅在标记有效时生效）
        if (DistortionManager.hasDistortion(living, DistortionManager.TYPE_MOVE_INVERT)) {
            Vec3 motion = living.getDeltaMovement();
            // 仅反转水平分量，避免反转重力导致原地抽搐
            living.setDeltaMovement(-motion.x * 0.6, motion.y, -motion.z * 0.6);
            living.hurtMarked = true;
            if (level.getGameTime() % 5 == 0) {
                level.sendParticles(ParticleTypes.WITCH,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
                        4, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }

    // =====================================================================
    // 攻击转移（E）与 隔绝房间内外交互阻断
    // =====================================================================

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        LivingEntity victim = event.getEntity();
        ServerLevel level = (ServerLevel) victim.level();

        // --- 隔绝房间：内外互相攻击阻断 ---
        BlockPos vPos = victim.blockPosition();
        boolean victimInside = DistortionManager.isInsideIsolate(level, vPos);
        Entity attacker = event.getSource().getEntity();
        boolean attackerInside = attacker != null
                && DistortionManager.isInsideIsolate(level, attacker.blockPosition());
        if (victimInside != attackerInside) {
            event.setCanceled(true);
            level.sendParticles(ParticleTypes.WITCH,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                    6, 0.4, 0.4, 0.4, 0.0);
            return;
        }

        // --- 攻击转移（E）---（仅对施法者本人的攻击生效）
        if (attacker instanceof LivingEntity attackerEntity
                && DistortionManager.hasDistortion(attackerEntity,
                        DistortionManager.TYPE_ATTACK_REDIRECT)) {
            UUID owner = DistortionManager.distortionOwner(attackerEntity);
            ServerPlayer caster = owner == null ? null
                    : level.getServer().getPlayerList().getPlayer(owner);
            if (caster != null && victim == caster) {
                event.setCanceled(true);
                // 把攻击者的攻击目标强制改为附近最近的其他生物
                if (attackerEntity instanceof Mob mob) {
                    LivingEntity newTarget = nearestOther(level, mob, caster);
                    mob.setTarget(newTarget);
                }
                level.sendParticles(ParticleTypes.WITCH,
                        attackerEntity.getX(), attackerEntity.getY() + attackerEntity.getBbHeight() * 0.5,
                        attackerEntity.getZ(), 10, 0.4, 0.4, 0.4, 0.0);
            }
        }
    }

    /** 施法者附近最近的敌对 / 任意其他生物（不含施法者与攻击者自身）。 */
    private static LivingEntity nearestOther(ServerLevel level, Mob attacker, ServerPlayer caster) {
        AABB box = caster.getBoundingBox().inflate(REDIRECT_SEARCH_RADIUS);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != caster && e != attacker && e.isAlive() && e instanceof Enemy)
                .stream().min(Comparator.comparingDouble(caster::distanceToSqr))
                .orElse(null);
    }

    // =====================================================================
    // 区域维护：进出阻挡 / 耐久 / 门联动 / 到期清理
    // =====================================================================

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.getGameTime() % ZONE_INTERVAL != 0) {
            return;
        }
        long now = level.getGameTime();
        DistortionZoneData data = level.getData(ModAttachments.DISTORTION_ZONES);
        if (data.rings().isEmpty()) {
            return;
        }

        for (DistortionZoneData.Ring ring : data.rings()) {
            if (ring.expiry() <= now) {
                continue;
            }
            // 封闭屏障：门被再次打开 → 屏障提前消失
            if (ring.type() == DistortionZoneData.TYPE_SEAL
                    && DistortionManager.isDoorOpen(level, ring.center())) {
                data.rings().remove(ring);
                level.setData(ModAttachments.DISTORTION_ZONES, data);
                return;
            }
            // 区域：阻挡内部实体穿出边界（封闭屏障） / 推出非授权进入者（隔绝房间）
            BlockPos a = ring.minPos();
            BlockPos b = ring.maxPos();
            AABB box = new AABB(a.getX(), a.getY(), a.getZ(),
                    b.getX() + 1.0, b.getY() + 1.0, b.getZ() + 1.0);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                    le -> le.isAlive() && !(le instanceof ServerPlayer)
                            || le instanceof ServerPlayer)) {
                handleRingEntity(level, ring, e, now);
            }
        }

        // 到期清理
        data.purgeExpired(now);
        level.setData(ModAttachments.DISTORTION_ZONES, data);
    }

    /** 区域对单个实体的进出处理。 */
    private static void handleRingEntity(ServerLevel level, DistortionZoneData.Ring ring,
                                         LivingEntity e, long now) {
        if (ring.type() == DistortionZoneData.TYPE_SEAL) {
            // 封闭屏障：实体位置在屏障盒内，若下一个位置将穿出边界则回推（无伤害，仅阻挡）
            Vec3 pos = e.position();
            Vec3 next = pos.add(e.getDeltaMovement());
            if (!ring.covers(BlockPos.containing(next)) && ring.covers(BlockPos.containing(pos))) {
                Vec3 back = pos.subtract(next).normalize().scale(SEAL_PUSH_BACK);
                e.setDeltaMovement(e.getDeltaMovement().add(back.x, 0.1, back.z));
                e.hurtMarked = true;
                level.sendParticles(ParticleTypes.WITCH,
                        e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
            }
        } else if (ring.type() == DistortionZoneData.TYPE_ISOLATE) {
            // 隔绝房间：非授权玩家被强制推出到边界外
            if (e instanceof ServerPlayer visitor
                    && !DistortionManager.canEnterIsolate(level, visitor, e.blockPosition())) {
                pushOutOfIsolate(level, ring, visitor);
            }
        }
    }

    /** 把进入隔绝房间的非授权玩家推回边界外。 */
    private static void pushOutOfIsolate(ServerLevel level, DistortionZoneData.Ring ring,
                                         ServerPlayer visitor) {
        BlockPos a = ring.minPos();
        BlockPos b = ring.maxPos();
        Vec3 pos = visitor.position();
        double cx = (a.getX() + b.getX()) / 2.0;
        double cz = (a.getZ() + b.getZ()) / 2.0;
        Vec3 out = pos.subtract(cx, 0, cz).normalize().scale(1.2);
        visitor.setDeltaMovement(out.x, 0.4, out.z);
        visitor.hurtMarked = true;
        if (visitor.getDeltaMovement().horizontalDistanceSqr() > 0.1) {
            visitor.teleportTo(level,
                    pos.x + out.x * 3, Math.max(a.getY(), pos.y), pos.z + out.z * 3,
                    visitor.getYRot(), visitor.getXRot());
        }
        visitor.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        "message.guimi_mod.distortion.isolate_blocked"), true);
        level.sendParticles(ParticleTypes.WITCH,
                visitor.getX(), visitor.getY() + 1.0, visitor.getZ(), 10, 0.4, 0.6, 0.4, 0.0);
    }

    // =====================================================================
    // 左键击破封闭屏障 / 隔绝房间
    // =====================================================================

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        if (!DistortionManager.isRingBoundary(level, pos)) {
            return;
        }
        // 左键攻击屏障边界方块：削减耐久并防止破坏方块
        boolean alive = DistortionManager.damageRing(level, pos, 5);
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            alive ? "message.guimi_mod.distortion.ring_hit"
                                    : "message.guimi_mod.distortion.ring_broken"), true);
        }
        level.sendParticles(ParticleTypes.CRIT,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.0);
    }
}
