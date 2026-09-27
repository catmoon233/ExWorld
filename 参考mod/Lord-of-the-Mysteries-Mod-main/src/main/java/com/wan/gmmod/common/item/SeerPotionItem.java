package com.wan.gmmod.common.item;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.characteristics.CharacteristicManager;
import com.wan.gmmod.content.ritual.PromotionRitualManager;
import com.wan.gmmod.content.sequences.Sequence;
import com.wan.gmmod.content.sequences.SequenceRegistry;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 普通序列魔药（序列 9~6）。晋升校验规则：
 * <ul>
 *   <li>正常晋升：当前途径与魔药途径一致，且当前序列等级恰为目标序列的前置等级；</li>
 *   <li>已就职后服用<b>其他途径</b>的魔药 → 失控（魔药被消耗）；</li>
 *   <li><b>越级</b>服用高品阶魔药（含凡人直接喝序列 8 及以上）→ 失控（魔药被消耗）；</li>
 *   <li>服用低于当前序列的魔药 → 无效，仅提示，不消耗。</li>
 * </ul>
 * 失控惩罚复用 {@link PromotionRitualManager#punishLosingControl}（失控 5 分钟 + 污染 +30）。
 */
public class SeerPotionItem extends Item {

    private final ResourceLocation targetSequenceId;


    public SeerPotionItem(Properties properties, ResourceLocation targetSequenceId) {
        super(properties);
        this.targetSequenceId = targetSequenceId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Sequence targetSeq = SequenceRegistry.get(targetSequenceId);

        if (targetSeq == null) {
                if (!level.isClientSide) {
                    player.sendSystemMessage(Component.literal("错误：魔药对应的序列不存在！"));
                    GuimiMod.LOGGER.error("尝试饮用魔药但找不到序列 {}", targetSequenceId);
                } return InteractionResultHolder.fail(stack);
        }

        int currentSeqLevel = player.getData(ModAttachments.SEQUENCE_LEVEL);
        Sequences.Pathway pathway = targetSeq.getPathway();

        // 失控：已就职后服用其他途径的魔药（非凡特性剧烈冲突）
        if (Sequences.employed(player.getData(ModAttachments.PATHWAY))
                && !pathway.getKey().equals(player.getData(ModAttachments.PATHWAY))) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                stack.shrink(1);
                PromotionRitualManager.punishLosingControl(sp,
                        "message.guimi_mod.potion.wrong_pathway");
            }
            return InteractionResultHolder.consume(stack);
        }
        // 服用当前及更低序列的魔药：无效，仅提示，不消耗
        if (Sequences.employed(player.getData(ModAttachments.PATHWAY)) && targetSeq.getLevel() >= currentSeqLevel) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.guimi_mod.cannot_promote"));
            }
            return InteractionResultHolder.fail(stack);
        }
        // 失控：越级服用高品阶魔药（含凡人直接喝序列 8 及以上）
        if (currentSeqLevel != targetSeq.getRequiredLevel()) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                stack.shrink(1);
                PromotionRitualManager.punishLosingControl(sp,
                        "message.guimi_mod.potion.overstep");
            }
            return InteractionResultHolder.consume(stack);
        }

        // 晋升条件：当前序列等级 == 目标序列所需的前置等级
        if (currentSeqLevel == targetSeq.getRequiredLevel()) {
            if (!level.isClientSide) {
                // 守恒定律：非首次晋升需消耗低序列特性（全局池验证防超量）
                if (Sequences.employed(player.getData(ModAttachments.PATHWAY))) {
                    int available = CharacteristicManager.pool(level).get(pathway, currentSeqLevel);
                    if (available <= 0) {
                        player.sendSystemMessage(Component.translatable("message.guimi_mod.characteristic_insufficient"));
                        return InteractionResultHolder.fail(stack);
                    }
                }
                // 晋升
                player.setData(ModAttachments.SEQUENCE_LEVEL, targetSeq.getLevel());
                player.setData(ModAttachments.ACTING_SEQUENCE_ID, targetSeq.getId().toString());
                // 记录玩家途径，供序列能力 / 占卜按真实途径匹配
                player.setData(ModAttachments.PATHWAY, pathway.getKey());
                if (SpiritualityManager.get(player) == 0) {
                    SpiritualityManager.set(player, 100);  // 首次就职获得灵性
                }
                stack.shrink(1); // 消耗一个物品

                // 守恒定律：全局池转移。首次就职（自 0 起）新增一份序列 9 特性；
                // 后续晋升将低序列特性转化为高序列特性（低 -1、高 +1）。
                if (currentSeqLevel == 0) {
                    CharacteristicManager.addToPool(level, pathway, targetSeq.getLevel(), 1);
                } else {
                    CharacteristicManager.transferUp(level, pathway, currentSeqLevel, targetSeq.getLevel());
                }

                // 注意：translatable 参数只允许 String/Number/Boolean/Component，
                // 直接传 Pathway 枚举会在网络序列化时报错导致玩家断线
                player.sendSystemMessage(Component.translatable("message.guimi_mod.promoted",
                        pathway.getDisplayName(), targetSeq.getLevel()));
                GuimiMod.LOGGER.info("玩家 {} 晋升至 {} 序列 {}", player.getName().getString(),
                        pathway.getDisplayName(), targetSeq.getLevel());
                // 晋升钩子：女巫「性别转换」等（男性晋升女巫自动切换女性形态）
                com.wan.gmmod.content.witch.PromotionHooks.onPromoted(player, targetSeq);
                // 任务钩子：上报「晋升」目标进度（主线任务衔接）
                if (player instanceof ServerPlayer sp) {
                    com.wan.gmmod.content.quest.QuestManager.onPromoted(sp, targetSeq.getId().toString());
                }
            }
            return InteractionResultHolder.success(stack);
        } else {
            // 服用低于当前序列的魔药：无效，仅提示
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.guimi_mod.cannot_promote"));
            }
            return InteractionResultHolder.fail(stack);
        }
    }
}