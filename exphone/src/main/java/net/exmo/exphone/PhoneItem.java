package net.exmo.exphone;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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

    public static int tint(ItemStack stack) {
        return PhoneStacks.model(stack).map(model -> model.color().argb()).orElse(0xFFC8C8C8);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.pick(4.5, 0, false) instanceof net.minecraft.world.phys.BlockHitResult hit
                && level.getBlockState(hit.getBlockPos()).getBlock() instanceof PhoneChargerBlock) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) PhoneStacks.ensureModel(stack, level.getRandom());
        if (level.isClientSide) PhoneClient.open();
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide) PhoneStacks.ensureModel(stack, level.getRandom());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String number = PhoneStacks.number(stack);
        tooltip.add(number.isBlank()
                ? Component.translatable("item.exphone.phone.no_sim")
                : Component.literal(number));
        tooltip.add(Component.translatable("item.exphone.phone.battery", PhoneStacks.battery(stack)));
        PhoneStacks.model(stack).ifPresentOrElse(
                model -> tooltip.add(Component.translatable("item.exphone.phone.model",
                        Component.translatable(model.brand().translationKey()),
                        Component.translatable(model.color().translationKey()))),
                () -> tooltip.add(Component.translatable("item.exphone.phone.unset"))
        );
    }
}
