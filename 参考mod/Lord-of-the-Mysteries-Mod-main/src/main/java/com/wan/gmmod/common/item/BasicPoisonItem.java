package com.wan.gmmod.common.item;

import com.wan.gmmod.common.capability.ModAttachments;
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
 * 基础毒药——猎人（战争之红途径 · 序列 9）「野外知识」的产物。
 * <p>
 * 右键使用后将毒药涂抹到武器上（{@code POISON_BLADE_HITS} = 3）：
 * 接下来 3 次近战命中使目标中毒 5 秒（判定在 WarAbilityEventSubscriber）。
 * 由潜行右键地面以「玻璃瓶 + 蜘蛛眼」野外制作。
 */
public class BasicPoisonItem extends Item {

    /** 一瓶毒药可附着的近战命中次数。 */
    public static final int COAT_HITS = 3;

    public BasicPoisonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.setData(ModAttachments.POISON_BLADE_HITS, COAT_HITS);
            player.sendSystemMessage(Component.translatable("message.guimi_mod.poison_coated", COAT_HITS));
            level.playSound(null, player.blockPosition(),
                    SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 1.4F);
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
