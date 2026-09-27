package com.wan.gmmod.common.item;

import com.wan.gmmod.content.ritual.PromotionRitualManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 仪式晋升物品：手持右键触发高序列「晋升仪式」（服务端执行）。
 * <p>
 * 序列5「秘偶大师」魔药与秘偶大师非凡特性共用本类——特性可作为魔药的等效替代品。
 * 检测与结算逻辑全部在 {@link PromotionRitualManager#attempt}：基础条件不满足仅提示；
 * 仪式条件（美人鱼的歌声）不满足则失控。
 */
public class RitualAdvanceItem extends Item {

    private final ResourceLocation targetSequenceId;

    public RitualAdvanceItem(Properties properties, ResourceLocation targetSequenceId) {
        super(properties);
        this.targetSequenceId = targetSequenceId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            boolean consumed = PromotionRitualManager.attempt(serverPlayer, stack, targetSequenceId);
            return consumed ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guimi_mod.ritual_advance.tooltip")
                .withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
