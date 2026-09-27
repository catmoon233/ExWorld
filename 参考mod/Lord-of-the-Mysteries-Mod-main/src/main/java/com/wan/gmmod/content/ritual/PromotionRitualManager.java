package com.wan.gmmod.content.ritual;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.registry.ModEffects;
import com.wan.gmmod.common.registry.ModSounds;
import com.wan.gmmod.content.characteristics.CharacteristicManager;
import com.wan.gmmod.content.effects.LosingControlEffect;
import com.wan.gmmod.content.sequences.Sequence;
import com.wan.gmmod.content.sequences.SequenceRegistry;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/**
 * 高序列「晋升仪式」通用系统。
 * <p>
 * 每个高序列魔药可配置独立的仪式条件；当前实现序列5「秘偶大师」：
 * <ul>
 *     <li>基础检测（不满足则无法服用，无惩罚）：当前序列等级 = 目标前置等级（6）、
 *     灵性 ≥ {@value #SPIRITUALITY_COST}、扮演进度 = 100、低序列特性充足（守恒定律）；</li>
 *     <li>仪式条件：处于「美人鱼的歌声」（或等效中和效果）的覆盖范围内且 Buff 为真；</li>
 *     <li>条件满足：消耗灵性与魔药，正常晋升并播放紫金色粒子、歌声短暂增强，污染不增加；</li>
 *     <li>条件不满足仍服用：扣灵性 {@value #SPIRITUALITY_COST}、不晋升、进入失控 5 分钟、
 *     污染 +{@value #FAILURE_POLLUTION}，魔药不返还。</li>
 * </ul>
 */
public final class PromotionRitualManager {

    /** 晋升消耗基础灵性 */
    public static final int SPIRITUALITY_COST = 100;
    /** 要求的扮演进度 */
    public static final int REQUIRED_ACTING = 100;
    /** 失控时增加的污染值 */
    public static final int FAILURE_POLLUTION = 30;

    private PromotionRitualManager() {}

    /**
     * 服务端执行晋升仪式检测与结算。
     *
     * @param player           服用者
     * @param stack            手中的魔药 / 非凡特性
     * @param targetSequenceId 目标序列（如 {@code guimi_mod:fool_5}）
     * @return 是否消耗了物品（成功晋升或失控都会消耗）
     */
    public static boolean attempt(ServerPlayer player, ItemStack stack, ResourceLocation targetSequenceId) {
        Sequence targetSeq = SequenceRegistry.get(targetSequenceId);
        if (targetSeq == null) {
            GuimiMod.LOGGER.error("晋升仪式找不到目标序列 {}", targetSequenceId);
            return false;
        }
        ServerLevel level = player.serverLevel();
        Sequences.Pathway pathway = targetSeq.getPathway();
        int currentLevel = player.getData(ModAttachments.SEQUENCE_LEVEL);

        // ===== 基础检测：不满足则根本无法服用（无惩罚） =====
        if (currentLevel != targetSeq.getRequiredLevel()) {
            player.sendSystemMessage(Component.translatable("message.guimi_mod.cannot_promote"));
            return false;
        }
        // 服用其他途径的魔药：非凡特性剧烈冲突，直接失控（魔药被消耗）
        if (!pathway.getKey().equals(player.getData(ModAttachments.PATHWAY))) {
            stack.shrink(1);
            punishLosingControl(player, "message.guimi_mod.potion.wrong_pathway");
            return true;
        }
        int spirituality = SpiritualityManager.get(player);
        if (spirituality < SPIRITUALITY_COST) {
            player.sendSystemMessage(Component.translatable("message.guimi_mod.ritual_no_spirituality",
                    SPIRITUALITY_COST));
            return false;
        }
        if (player.getData(ModAttachments.ACTING_PROGRESS) < REQUIRED_ACTING) {
            player.sendSystemMessage(Component.translatable("message.guimi_mod.ritual_acting_incomplete"));
            return false;
        }
        // 守恒定律：晋升需消耗低序列特性（全局池验证防超量）
        if (CharacteristicManager.pool(level).get(pathway, currentLevel) <= 0) {
            player.sendSystemMessage(Component.translatable("message.guimi_mod.characteristic_insufficient"));
            return false;
        }

        // ===== 仪式条件：美人鱼的歌声（真伪由服务端悄悄判定） =====
        SpiritualityManager.set(player, spirituality - SPIRITUALITY_COST);
        stack.shrink(1);

        if (!MermaidSongManager.isRitualConditionMet(player)) {
            punishLosingControl(player);
            return true;
        }

        // ===== 正常晋升 =====
        player.setData(ModAttachments.SEQUENCE_LEVEL, targetSeq.getLevel());
        player.setData(ModAttachments.ACTING_SEQUENCE_ID, targetSeq.getId().toString());
        player.setData(ModAttachments.PATHWAY, pathway.getKey());
        player.setData(ModAttachments.ACTING_PROGRESS, 0);
        CharacteristicManager.transferUp(level, pathway, currentLevel, targetSeq.getLevel());

        // 紫金色光芒包围玩家 + 美人鱼歌声短暂增强
        DustParticleOptions gold = new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.1F), 1.2F);
        level.sendParticles(ParticleTypes.WITCH,
                player.getX(), player.getY() + 1.0, player.getZ(), 80, 0.8, 1.0, 0.8, 0.05);
        level.sendParticles(gold,
                player.getX(), player.getY() + 1.0, player.getZ(), 80, 0.8, 1.0, 0.8, 0.0);
        level.playSound(null, player.blockPosition(), ModSounds.MERMAID_SONG.get(),
                SoundSource.PLAYERS, 1.6F, 1.0F);

        player.sendSystemMessage(Component.translatable("message.guimi_mod.ritual_promoted",
                pathway.getDisplayName(), targetSeq.getLevel(), targetSeq.getName()));
        GuimiMod.LOGGER.info("玩家 {} 通过晋升仪式晋升至 {} 序列 {}",
                player.getName().getString(), pathway.getDisplayName(), targetSeq.getLevel());
        // 任务钩子：上报「晋升」目标进度（主线任务衔接）
        com.wan.gmmod.content.quest.QuestManager.onPromoted(player, targetSeq.getId().toString());
        return true;
    }

    /** 失控惩罚：不晋升、失控 5 分钟、污染 +30，魔药不返还。 */
    private static void punishLosingControl(ServerPlayer player) {
        punishLosingControl(player, "message.guimi_mod.ritual_losing_control");
    }

    /**
     * 失控惩罚（通用入口）：失控 5 分钟、污染 +30，并发送指定提示。
     * 供仪式失败与错误服药（跨途径 / 越级魔药，见 {@code SeerPotionItem}）复用。
     */
    public static void punishLosingControl(ServerPlayer player, String messageKey) {
        player.addEffect(new MobEffectInstance(ModEffects.LOSING_CONTROL,
                LosingControlEffect.DEFAULT_DURATION, 0));
        player.sendSystemMessage(Component.translatable(messageKey));
        GuimiMod.LOGGER.info("玩家 {} 进入失控状态", player.getName().getString());
    }
}
