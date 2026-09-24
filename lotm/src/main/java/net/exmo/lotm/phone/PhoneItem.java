package net.exmo.lotm.phone;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class PhoneItem extends Item {
    public PhoneItem(Properties properties) {
        super(properties);
    }

     @Override
     public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
         ItemStack stack = player.getItemInHand(hand);
         if (player.pick(4.5, 0, false) instanceof net.minecraft.world.phys.BlockHitResult hit
                 && level.getBlockState(hit.getBlockPos()).getBlock() instanceof PhoneChargerBlock) {
             return InteractionResultHolder.pass(stack);
         }
         if (level.isClientSide) PhoneClient.open();
         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
     }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String number = PhoneStacks.number(stack);
         tooltip.add(Component.literal(number.isBlank() ? "未安装 SIM" : number));
         tooltip.add(Component.literal("电量 " + PhoneStacks.battery(stack) + "%"));
        PhoneStacks.model(stack).ifPresentOrElse(
                model -> tooltip.add(Component.literal(model.brand().label() + " · " + model.color().label())),
                () -> tooltip.add(Component.literal("未设定型号"))
        );
    }
}
