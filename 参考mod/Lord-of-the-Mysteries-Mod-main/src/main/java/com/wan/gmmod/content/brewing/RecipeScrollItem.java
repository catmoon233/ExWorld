package com.wan.gmmod.content.brewing;

import com.wan.gmmod.common.capability.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

/**
 * 魔药配方卷轴：全部序列的魔药配方共用同一种物品类与同一张卷轴纹理，
 * 仅靠注册名 / 显示名 / tooltip 区分（避免为每个配方单独制作纹理与物品类）。
 * <p>
 * 右键研读后永久掌握对应魔药的炼制方法（写入 {@link RecipeKnowledgeData} 附件），
 * 此后方可在炼药锅中合成该魔药；研读会消耗卷轴。
 * <p>
 * 获取途径：遗迹宝箱、NPC 处购买、任务奖励，部分配方自然生成于特定结构。
 */
public class RecipeScrollItem extends Item {

    /** 对应的魔药物品注册名（同时作为配方 ID，如 {@code "seer_potion"}）。 */
    public final String potionId;

    public RecipeScrollItem(String potionId, Properties properties) {
        super(properties);
        this.potionId = potionId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            RecipeKnowledgeData known = server.getData(ModAttachments.READ_RECIPES);
            if (known.contains(potionId)) {
                server.displayClientMessage(
                        Component.translatable("message.guimi_mod.recipe_already"), true);
                return InteractionResultHolder.fail(stack);
            }
            server.setData(ModAttachments.READ_RECIPES, known.with(potionId));
            stack.shrink(1);
            level.playSound(null, server.blockPosition(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.2F);
            server.displayClientMessage(Component.translatable(
                    "message.guimi_mod.recipe_learned",
                    Component.translatable("item.guimi_mod." + potionId)), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.guimi_mod.recipe_scroll.usage")
                .withStyle(ChatFormatting.GRAY));
        BrewingRecipe recipe = BrewingRecipeManager.byId(potionId);
        if (recipe == null) {
            super.appendHoverText(stack, context, tooltip, flag);
            return;
        }
        tooltip.add(Component.translatable("tooltip.guimi_mod.recipe_scroll.ingredients")
                .withStyle(ChatFormatting.GOLD));
        for (Map.Entry<net.minecraft.world.item.Item, Integer> e : recipe.ingredients().entrySet()) {
            tooltip.add(Component.literal("· " + e.getKey().getDescription().getString()
                    + " x" + e.getValue()).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** 供创造模式 / 指令查询：该卷轴对应配方是否已被玩家掌握。 */
    public static boolean isKnown(Player player, ItemStack stack) {
        return stack.getItem() instanceof RecipeScrollItem scroll
                && player.getData(ModAttachments.READ_RECIPES).contains(scroll.potionId);
    }
}
