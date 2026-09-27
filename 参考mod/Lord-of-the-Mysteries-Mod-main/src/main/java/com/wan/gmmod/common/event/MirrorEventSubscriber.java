package com.wan.gmmod.common.event;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.InterferenceFieldData;
import com.wan.gmmod.content.divination.SpiritCommune;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

/**
 * 魔镜占卜（反占卜 / 通灵）事件监听：
 * <ul>
 *   <li><b>干扰场维护</b>：定期清理过期干扰场，并向附近开启灵视的高序列
 *   玩家定向发送淡红色雾状粒子（普通玩家不可见）；</li>
 *   <li><b>复仇任务</b>：玩家击杀通灵指定的怨灵后发放奖励；</li>
 *   <li><b>埋骨任务</b>：玩家携带遗骨抵达指定群系后发放奖励。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class MirrorEventSubscriber {
    /** 干扰场维护周期（tick） */
    private static final int FIELD_INTERVAL = 40;
    /** 灵视雾粒子可见距离 */
    private static final double VISION_RANGE = 48.0;
    /** 高序列灵视门槛：序列 6 及以上（数字 ≤ 6）可见干扰场 */
    private static final int VISION_SEQ = 6;
    /** 淡红色雾状粒子 */
    private static final DustParticleOptions RED_MIST =
            new DustParticleOptions(new Vector3f(0.9F, 0.3F, 0.3F), 1.5F);

    // ===== 干扰场：过期清理 + 高序列灵视雾粒子 =====

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.getGameTime() % FIELD_INTERVAL != 0) {
            return;
        }
        if (!level.hasData(ModAttachments.INTERFERENCE_FIELDS)) {
            return;
        }
        InterferenceFieldData data = level.getData(ModAttachments.INTERFERENCE_FIELDS);
        long now = level.getGameTime();
        data.purgeExpired(now);
        if (data.fields().isEmpty()) {
            return;
        }
        for (InterferenceFieldData.Field field : data.fields()) {
            BlockPos center = BlockPos.of(field.pos());
            for (ServerPlayer sp : level.players()) {
                if (!sp.getData(ModAttachments.SPIRIT_VISION)) {
                    continue;
                }
                int seq = sp.getData(ModAttachments.SEQUENCE_LEVEL);
                if (seq <= 0 || seq > VISION_SEQ
                        || sp.distanceToSqr(center.getCenter()) > VISION_RANGE * VISION_RANGE) {
                    continue;
                }
                sendMistParticles(level, sp, center);
            }
        }
    }

    /** 向单个灵视玩家定向发送干扰场范围内的淡红色雾粒子。 */
    private static void sendMistParticles(ServerLevel level, ServerPlayer sp, BlockPos center) {
        RandomSource random = level.random;
        int radius = InterferenceFieldData.RADIUS;
        for (int i = 0; i < 12; i++) {
            double x = center.getX() + 0.5 + (random.nextDouble() * 2 - 1) * radius;
            double y = center.getY() + 0.5 + (random.nextDouble() * 2 - 1) * radius;
            double z = center.getZ() + 0.5 + (random.nextDouble() * 2 - 1) * radius;
            level.sendParticles(sp, RED_MIST, true, x, y, z, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    // ===== 复仇任务：击杀通灵指定的怨灵 =====

    @SubscribeEvent
    public static void onRevengeKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)
                || sp.level().isClientSide) {
            return;
        }
        String task = sp.getData(ModAttachments.SPIRIT_TASK);
        String expected = SpiritCommune.TASK_REVENGE + ";" + event.getEntity().getUUID();
        if (task.equals(expected)) {
            SpiritCommune.completeTask(sp, SpiritCommune.TASK_REVENGE);
        }
    }

    // ===== 埋骨任务：携带遗骨抵达指定群系 =====

    @SubscribeEvent
    public static void onBurialTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)
                || sp.tickCount % 20 != 0) {
            return;
        }
        String task = sp.getData(ModAttachments.SPIRIT_TASK);
        if (!task.startsWith(SpiritCommune.TASK_BURIAL + ";")) {
            return;
        }
        String token = task.substring(task.indexOf(';') + 1);
        ServerLevel level = sp.serverLevel();
        if (!SpiritCommune.matchesBiomeToken(level, sp.blockPosition(), token)) {
            return;
        }
        // 消耗一件遗骨，完成埋葬
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack stack = sp.getInventory().getItem(i);
            if (SpiritCommune.isRelic(stack)) {
                stack.shrink(1);
                SpiritCommune.completeTask(sp, SpiritCommune.TASK_BURIAL);
                return;
            }
        }
    }
}
