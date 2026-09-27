package com.wan.gmmod.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 止血药膏——猎人（战争之红途径 · 序列 9）「野外知识」的产物。
 * <p>
 * 右键使用后瞬间恢复 2 颗心（4 点生命），消耗一个。
 * 由潜行右键地面以「纸 + 甜浆果 x2」野外制作（见 WarAbilityEventSubscriber）。
 */
public class HemostaticSalveItem extends Item {

    public HemostaticSalveItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getHealth() >= player.getMaxHealth()) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.guimi_mod.salve_full_health"));
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            player.heal(4.0F);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 1.0F, 1.2F);
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
