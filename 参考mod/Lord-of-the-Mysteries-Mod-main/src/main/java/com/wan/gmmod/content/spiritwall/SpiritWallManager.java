package com.wan.gmmod.content.spiritwall;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.entities.WraithEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 灵性之墙管理器：负责服务端灵性之墙的激活、维持、消耗与碰撞阻挡。
 * <p>
 * 灵性之墙以玩家为中心生成 9×9×9 的透明粒子屏障（稀疏粒子），
 * 持续消耗灵性值（每秒 2 点），阻挡怪物与灵体生物（如 Wraith）进入。
 * 灵性不足时自动解除。
 */
public class SpiritWallManager {

    /** 灵性之墙每秒消耗灵性值 */
    private static final int COST_PER_SECOND = 2;
    /** 灵性之墙每 tick 消耗间隔（20 tick = 1 秒） */
    private static final int COST_INTERVAL = 20;
    /** 墙体半边长（9×9×9 → 中心向外 4 格） */
    private static final int HALF_SIZE = 4;
    /** 每次刷新时在墙面随机生成的粒子数（稀疏化） */
    private static final int PARTICLES_PER_SPAWN = 12;

    /** 活跃的灵性之墙：玩家 UUID → 墙中心坐标 */
    private static final Map<UUID, BlockPos> ACTIVE_WALLS = new HashMap<>();

    /** 切换灵性之墙状态。 */
    public static void toggle(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (ACTIVE_WALLS.containsKey(uuid)) {
            // 解除
            deactivate(player);
        } else {
            // 激活前检查灵性
            int spirituality = SpiritualityManager.get(player);
            if (spirituality < COST_PER_SECOND) {
                player.displayClientMessage(
                        Component.translatable("message.guimi_mod.spirit_wall_no_energy"), true);
                return;
            }
            activate(player);
        }
    }

    /** 激活灵性之墙。 */
    private static void activate(ServerPlayer player) {
        BlockPos center = player.blockPosition();
        ACTIVE_WALLS.put(player.getUUID(), center);
        player.displayClientMessage(
                Component.translatable("message.guimi_mod.spirit_wall_activated"), true);
    }

    /** 解除灵性之墙。 */
    public static void deactivate(ServerPlayer player) {
        ACTIVE_WALLS.remove(player.getUUID());
        player.displayClientMessage(
                Component.translatable("message.guimi_mod.spirit_wall_deactivated"), true);
    }

    /** 判断某玩家是否拥有活跃的灵性之墙。 */
    public static boolean isActive(ServerPlayer player) {
        return ACTIVE_WALLS.containsKey(player.getUUID());
    }

    /** 获取灵性之墙中心位置。 */
    public static BlockPos getWallCenter(UUID playerUUID) {
        return ACTIVE_WALLS.get(playerUUID);
    }

    /** 判断某位置是否处于任意活跃灵性之墙内（供祭台仪式校验）。 */
    public static boolean isInsideAnyWall(BlockPos pos) {
        for (BlockPos center : ACTIVE_WALLS.values()) {
            if (Math.abs(pos.getX() - center.getX()) <= HALF_SIZE
                    && Math.abs(pos.getY() - center.getY()) <= HALF_SIZE
                    && Math.abs(pos.getZ() - center.getZ()) <= HALF_SIZE) {
                return true;
            }
        }
        return false;
    }

    /**
     * 每 tick 调用：维持灵性之墙效果。
     * - 消耗灵性（每秒 2 点）
     * - 生成粒子
     * - 阻挡灵体
     * - 灵性不足时自动解除
     */
    public static void tickPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (!ACTIVE_WALLS.containsKey(uuid)) return;

        BlockPos center = ACTIVE_WALLS.get(uuid);
        ServerLevel level = player.serverLevel();

        // 每 20 tick 消耗一次灵性
        if (player.tickCount % COST_INTERVAL == 0) {
            int spirituality = SpiritualityManager.get(player);
            if (spirituality < COST_PER_SECOND) {
                deactivate(player);
                return;
            }
            SpiritualityManager.set(player, spirituality - COST_PER_SECOND);
        }

        // 每 10 tick 在 9×9×9 边界随机生成少量粒子
        if (player.tickCount % 10 == 0) {
            spawnWallParticles(level, center);
        }

        // 阻挡怪物与灵体穿过（每 tick 检测）
        blockMonsters(level, center);
    }

    /** 在 9×9×9 区域边界随机位置生成少量透明粒子（稀疏化）。 */
    private static void spawnWallParticles(ServerLevel level, BlockPos center) {
        double cx = center.getX() + 0.5;
        double cy = center.getY() + 0.5;
        double cz = center.getZ() + 0.5;
        RandomSource random = level.getRandom();

        for (int i = 0; i < PARTICLES_PER_SPAWN; i++) {
            // 随机选一个面，在该面上随机取点
            int face = random.nextInt(6);
            double a = (random.nextDouble() * 2 - 1) * HALF_SIZE;
            double b = (random.nextDouble() * 2 - 1) * HALF_SIZE;
            double x, y, z;
            switch (face) {
                case 0 -> { x = HALF_SIZE;  y = a; z = b; }
                case 1 -> { x = -HALF_SIZE; y = a; z = b; }
                case 2 -> { x = a; y = HALF_SIZE;  z = b; }
                case 3 -> { x = a; y = -HALF_SIZE; z = b; }
                case 4 -> { x = a; y = b; z = HALF_SIZE; }
                default -> { x = a; y = b; z = -HALF_SIZE; }
            }
            level.sendParticles(ParticleTypes.END_ROD,
                    cx + x, cy + y, cz + z,
                    1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** 阻挡怪物与灵体生物进入墙内。 */
    private static void blockMonsters(ServerLevel level, BlockPos center) {
        AABB wallBox = new AABB(
                center.getX() - HALF_SIZE + 0.5, center.getY() - HALF_SIZE + 0.5, center.getZ() - HALF_SIZE + 0.5,
                center.getX() + HALF_SIZE + 0.5, center.getY() + HALF_SIZE + 0.5, center.getZ() + HALF_SIZE + 0.5
        );

        // 查找墙范围内的怪物 / 灵体并弹开
        for (Entity entity : level.getEntities(null, wallBox)) {
            if (entity instanceof WraithEntity || entity instanceof Monster) {
                // 将其推出墙外
                Vec3 diff = entity.position().subtract(
                        center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
                Vec3 push = diff.normalize().scale(0.5);
                entity.setDeltaMovement(push);
                entity.hurtMarked = true;
            }
        }
    }

    /** 玩家断线或死亡时清理。 */
    public static void cleanup(UUID playerUUID) {
        ACTIVE_WALLS.remove(playerUUID);
    }
}
