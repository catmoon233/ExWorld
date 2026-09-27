package com.wan.gmmod.content.witch;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.abilities.SkillManager;
import com.wan.gmmod.content.abilities.WitchInvisibilityAbility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * 女巫途径持续状态维护器（服务端，由 {@code GameEventSubscriber#onPlayerTick} 每刻驱动）。
 * <ul>
 *   <li><b>隐形</b>：抬手抹脸后 2 秒过渡（喷发粒子），随后完全隐身 30 秒；
 *   期间持续刷新隐身效果，到期自动解除。攻击 / 受伤的中断在
 *   {@code WitchAbilityEventSubscriber} 中清空计时。</li>
 *   <li><b>蛛丝蚕茧</b>：5 秒内定身 + 生命恢复（无敌与破茧判定在事件订阅器）。</li>
 *   <li><b>冰霜护甲</b>：期间周期性喷发雪花粒子（减伤在事件订阅器）。</li>
 * </ul>
 */
public final class WitchPathwayManager {
    private WitchPathwayManager() {}

    public static void tickPlayer(ServerPlayer player) {
        tickInvisibility(player);
        tickCocoon(player);
        tickFrostArmor(player);
    }

    // ===== 隐形 =====

    private static void tickInvisibility(ServerPlayer player) {
        long now = player.level().getGameTime();
        long start = player.getData(ModAttachments.WITCH_INVIS_START);
        long end = player.getData(ModAttachments.WITCH_INVIS_END);

        if (start > 0L) {
            long elapsed = now - start;
            // 过渡阶段：喷发烟雾表现"身体逐渐消失"
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        3, 0.3, 0.6, 0.3, 0.01);
            }
            if (elapsed >= WitchInvisibilityAbility.TRANSITION_TICKS) {
                // 过渡结束 → 进入完全隐身
                player.setData(ModAttachments.WITCH_INVIS_START, 0L);
                player.setData(ModAttachments.WITCH_INVIS_END, now + WitchInvisibilityAbility.DURATION_TICKS);
                end = now + WitchInvisibilityAbility.DURATION_TICKS;
            }
        }

        if (end > 0L) {
            if (now >= end) {
                player.setData(ModAttachments.WITCH_INVIS_END, 0L);
                player.removeEffect(MobEffects.INVISIBILITY);
                player.displayClientMessage(Component.translatable("message.guimi_mod.witch_invisibility.end"), true);
            } else {
                // 持续刷新完全隐身（隐藏图标）
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false));
            }
        }
    }

    // ===== 蛛丝蚕茧 =====

    private static void tickCocoon(ServerPlayer player) {
        long now = player.level().getGameTime();
        long end = player.getData(ModAttachments.COCOON_END);
        if (end <= 0L) {
            return;
        }
        if (now >= end) {
            player.setData(ModAttachments.COCOON_END, 0L);
            return;
        }
        // 定身（近似"无法移动"）+ 生命恢复
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 250, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 10, 2, true, false));
        player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
        player.hurtMarked = true;
        if (player.level() instanceof ServerLevel level && now % 5 == 0) {
            level.sendParticles(ParticleTypes.ITEM_COBWEB,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    5, 0.4, 0.8, 0.4, 0.01);
        }
    }

    // ===== 冰霜护甲 =====

    private static void tickFrostArmor(ServerPlayer player) {
        long now = player.level().getGameTime();
        long end = player.getData(ModAttachments.FROST_ARMOR_END);
        if (end <= 0L) {
            return;
        }
        if (now >= end) {
            player.setData(ModAttachments.FROST_ARMOR_END, 0L);
            return;
        }
        if (player.level() instanceof ServerLevel level && now % 10 == 0) {
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    4, 0.4, 0.8, 0.4, 0.01);
        }
    }

    /** 该玩家当前是否处于蚕茧无敌状态。 */
    public static boolean isInCocoon(ServerPlayer player) {
        return player.getData(ModAttachments.COCOON_END) > player.level().getGameTime();
    }

    /** 该玩家当前是否覆盖冰霜护甲。 */
    public static boolean hasFrostArmor(ServerPlayer player) {
        return player.getData(ModAttachments.FROST_ARMOR_END) > player.level().getGameTime();
    }

    /** 兼容引用（避免未使用告警）：技能解锁查询工具。 */
    static boolean unlocked(ServerPlayer player, String path) {
        return SkillManager.isUnlocked(player, GuimiMod.id(path));
    }
}
