package com.wan.gmmod.content.meditation;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.network.packet.MeditationSyncPacket;
import com.wan.gmmod.content.spiritwall.SpiritWallManager;
import com.wan.gmmod.content.spirituality.SpiritualityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 冥想管理器：管理玩家的冥想状态。
 * <p>
 * 冥想规则：
 * - 按 P 键或潜行右键祭台开始/结束冥想
 * - 冥想时玩家站立闭眼（失明效果），无法移动，视角固定（客户端同步）
 * - 每 2 秒恢复灵性值（基础 2 点/2秒，灵性之墙内加成为 4 点/2秒）
 * - 灵性值满自动结束冥想
 * - 受到伤害自动中断冥想
 */
public class MeditationManager {

    /** 基础恢复量（每 2 秒） */
    private static final int BASE_RECOVERY = 2;
    /** 灵性之墙加成恢复量（每 2 秒） */
    private static final int WALL_BONUS_RECOVERY = 4;
    /** 恢复间隔（tick），2 秒 = 40 tick */
    private static final int RECOVERY_INTERVAL = 40;

    /** 正在冥想的玩家：UUID → 冥想开始时的位置 */
    private static final Map<UUID, BlockPos> MEDITATING_PLAYERS = new HashMap<>();

    /** 切换冥想状态。 */
    public static void toggle(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (MEDITATING_PLAYERS.containsKey(uuid)) {
            stopMeditation(player);
        } else {
            startMeditation(player);
        }
    }

    /** 开始冥想。 */
    private static void startMeditation(ServerPlayer player) {
        // 检查灵性是否已满（上限随序列成长）
        int spirituality = SpiritualityManager.get(player);
        int maxSpirituality = SpiritualityManager.getMax(player);
        if (spirituality >= maxSpirituality) {
            player.displayClientMessage(
                    Component.translatable("message.guimi_mod.meditation_full"), true);
            return;
        }

        MEDITATING_PLAYERS.put(player.getUUID(), player.blockPosition());
        // 同步客户端：锁定输入与视角
        PacketDistributor.sendToPlayer(player, new MeditationSyncPacket(true));
        player.displayClientMessage(
                Component.translatable("message.guimi_mod.meditation_start"), true);
    }

    /** 结束冥想。 */
    public static void stopMeditation(ServerPlayer player) {
        MEDITATING_PLAYERS.remove(player.getUUID());
        // 同步客户端解除姿态，并移除闭眼效果
        PacketDistributor.sendToPlayer(player, new MeditationSyncPacket(false));
        player.removeEffect(MobEffects.BLINDNESS);
        player.displayClientMessage(
                Component.translatable("message.guimi_mod.meditation_end"), true);
    }

    /** 判断某玩家是否正在冥想。 */
    public static boolean isMeditating(ServerPlayer player) {
        return MEDITATING_PLAYERS.containsKey(player.getUUID());
    }

    /**
     * 每 tick 调用：维持冥想效果。
     * - 阻止玩家移动
     * - 定时恢复灵性
     * - 灵性满后自动结束
     */
    public static void tickPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (!MEDITATING_PLAYERS.containsKey(uuid)) return;

        BlockPos meditationPos = MEDITATING_PLAYERS.get(uuid);

        // 检查玩家是否移动了（被打或其他原因）
        if (!player.blockPosition().equals(meditationPos)) {
            // 允许微小偏移（0.5 格），超过则中断
            double distSq = player.blockPosition().distSqr(meditationPos);
            if (distSq > 2.0) {
                stopMeditation(player);
                return;
            }
        }

        // 冻结玩家移动
        player.setDeltaMovement(0, player.getDeltaMovement().y, 0);

        // 闭眼姿态：周期性刷新失明效果（无粒子、无图标）
        if (player.tickCount % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false, false));
        }

        // 每 40 tick（2 秒）恢复灵性
        if (player.tickCount % RECOVERY_INTERVAL == 0) {
            int spirituality = SpiritualityManager.get(player);
            int maxSpirituality = SpiritualityManager.getMax(player);

            if (spirituality >= maxSpirituality) {
                // 灵性已满，自动结束冥想
                stopMeditation(player);
                return;
            }

            // 判断是否在灵性之墙内（获得加成）
            int recovery = BASE_RECOVERY;
            if (SpiritWallManager.isActive(player)) {
                recovery = WALL_BONUS_RECOVERY;
            }

            int newSpirituality = Math.min(spirituality + recovery, maxSpirituality);
            SpiritualityManager.set(player, newSpirituality);
            // 冥想修炼：累计周期，长期提升灵性上限
            SpiritualityManager.addTrainingCycle(player);
        }
    }

    /** 玩家受伤时中断冥想。 */
    public static void onPlayerHurt(ServerPlayer player) {
        if (MEDITATING_PLAYERS.containsKey(player.getUUID())) {
            stopMeditation(player);
            player.displayClientMessage(
                    Component.translatable("message.guimi_mod.meditation_interrupted"), true);
        }
    }

    /** 玩家断线或死亡时清理。 */
    public static void cleanup(UUID playerUUID) {
        MEDITATING_PLAYERS.remove(playerUUID);
    }
}
