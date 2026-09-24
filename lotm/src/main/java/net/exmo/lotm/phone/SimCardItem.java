package net.exmo.lotm.phone;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class SimCardItem extends Item {
    public SimCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack sim = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(sim);
        if (!(player instanceof ServerPlayer server)) return InteractionResultHolder.fail(sim);
        ItemStack phone = PhoneStacks.heldPhone(player);
        if (phone.isEmpty()) {
            player.displayClientMessage(Component.literal("请手持要安装 SIM 的手机"), true);
            return InteractionResultHolder.fail(sim);
        }
        PhoneData data = PhoneData.get(server);
        String existing = data.numberOf(server);
        if (!existing.isBlank() && !player.isShiftKeyDown()) {
            player.displayClientMessage(Component.literal("已有手机号 " + existing + "，潜行使用可更换"), true);
            return InteractionResultHolder.fail(sim);
        }
        String number = PhoneStacks.number(sim);
        if (number.isBlank()) number = data.freshNumber();
        String error = data.install(server, number, player.isShiftKeyDown());
        if (!error.isBlank()) {
            player.displayClientMessage(Component.literal(error), true);
            return InteractionResultHolder.fail(sim);
        }
        if (player.isShiftKeyDown()) PhoneStacks.clearMatching(player, existing);
        PhoneStacks.setNumber(phone, number);
        if (!player.getAbilities().instabuild) sim.shrink(1);
        player.displayClientMessage(Component.literal("已安装 SIM " + number), true);
        return InteractionResultHolder.consume(sim);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String number = PhoneStacks.number(stack);
        tooltip.add(Component.literal(number.isBlank() ? "右键装入手持的手机" : number));
    }
}
