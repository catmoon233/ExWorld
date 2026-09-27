package com.wan.gmmod.content.disguise;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.DisguiseData;
import com.wan.gmmod.common.capability.data.DisguiseUnlocks;
import com.wan.gmmod.content.abilities.SkillManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

/**
 * 变形系统服务端逻辑（无面人 · 序列 6）。
 * <p>
 * 负责：外观解锁（击杀 / 观察 / 初始赠送）、变形状态写入与校验。
 * 所有写入都走已同步的附件（{@link ModAttachments#DISGUISE_STATE}、
 * {@link ModAttachments#UNLOCKED_MOB_DISGUISES}），客户端渲染据此切换模型。
 * <p>
 * 平衡约束：变形只改变外观，不改变碰撞箱、名称标签、声音与战斗数值；
 * 观众（空想家途径）可以看穿变形（见客户端 DisguiseRenderHandler）。
 */
public final class DisguiseManager {
    private DisguiseManager() {}

    /** 变形能力 ID（与 DisguiseAbility 一致），用于校验玩家是否解锁序列 6 变形 */
    public static final ResourceLocation DISGUISE_ABILITY_ID = GuimiMod.id("disguise");

    /** 玩家是否拥有变形能力（无面人 · 序列 6 及以上）。 */
    public static boolean hasDisguiseAbility(Player player) {
        return SkillManager.isUnlocked(player, DISGUISE_ABILITY_ID);
    }

    /**
     * 解锁指定人形怪物外观（服务端）。
     * 非人形怪物直接忽略；重复解锁不提示。
     *
     * @param silent 为 true 时不向玩家发送解锁提示
     * @return 是否发生了新的解锁
     */
    public static boolean unlock(Player player, ResourceLocation mobId, boolean silent) {
        if (player.level().isClientSide || !HumanoidDisguises.isHumanoid(mobId)) {
            return false;
        }
        DisguiseUnlocks unlocks = player.getData(ModAttachments.UNLOCKED_MOB_DISGUISES);
        if (unlocks.contains(mobId)) {
            return false;
        }
        player.setData(ModAttachments.UNLOCKED_MOB_DISGUISES, unlocks.with(mobId));
        if (!silent) {
            HumanoidDisguises.Entry entry = HumanoidDisguises.get(mobId);
            Component name = entry.type().getDescription();
            player.displayClientMessage(
                    Component.translatable("disguise.guimi_mod.unlocked", name), false);
        }
        return true;
    }

    /** 初始赠送：拥有变形能力却还没有任何外观时，赠送僵尸 + 骷髅基础外观。 */
    public static void grantInitialGifts(Player player) {
        if (player.level().isClientSide || !hasDisguiseAbility(player)) {
            return;
        }
        DisguiseUnlocks unlocks = player.getData(ModAttachments.UNLOCKED_MOB_DISGUISES);
        if (!unlocks.ids().isEmpty()) {
            return;
        }
        for (ResourceLocation gift : HumanoidDisguises.INITIAL_GIFTS) {
            unlock(player, gift, true);
        }
        player.displayClientMessage(
                Component.translatable("disguise.guimi_mod.initial_gift"), false);
    }

    /**
     * 服务端设置变形目标（{@code mobId} 为 null 表示恢复原样）。
     * 校验：能力解锁、人形白名单、图鉴已解锁，防止客户端作弊。
     */
    public static void setDisguise(Player player, ResourceLocation mobId) {
        if (player.level().isClientSide) {
            return;
        }
        // 恢复原样：不需要额外校验
        if (mobId == null) {
            if (player.getData(ModAttachments.DISGUISE_STATE).isDisguised()) {
                player.setData(ModAttachments.DISGUISE_STATE, DisguiseData.none());
                playMorphEffects(player);
                player.displayClientMessage(
                        Component.translatable("disguise.guimi_mod.revert"), true);
            }
            return;
        }
        if (!hasDisguiseAbility(player)) {
            player.displayClientMessage(Component.translatable("message.guimi_mod.skill.locked"), true);
            return;
        }
        if (!HumanoidDisguises.isHumanoid(mobId)) {
            player.displayClientMessage(
                    Component.translatable("disguise.guimi_mod.not_humanoid"), true);
            return;
        }
        if (!player.getData(ModAttachments.UNLOCKED_MOB_DISGUISES).contains(mobId)) {
            player.displayClientMessage(
                    Component.translatable("disguise.guimi_mod.not_unlocked"), true);
            return;
        }
        player.setData(ModAttachments.DISGUISE_STATE, DisguiseData.ofMob(mobId));
        playMorphEffects(player);
        EntityType<?> type = HumanoidDisguises.get(mobId).type();
        player.displayClientMessage(
                Component.translatable("disguise.guimi_mod.morph", type.getDescription()), true);
    }

    /** 变形 / 恢复的烟雾与音效反馈。 */
    private static void playMorphEffects(Player player) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 1.2F);
        }
    }
}
