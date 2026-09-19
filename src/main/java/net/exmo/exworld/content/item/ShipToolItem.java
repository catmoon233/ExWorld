package net.exmo.exworld.content.item;

import net.exmo.exworld.ship.ShipSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Administrator capture wand. Permission is enforced on the server. */
public final class ShipToolItem extends Item {
    public ShipToolItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (context.getPlayer() instanceof ServerPlayer player) {
            ShipSystem.useTool(player, context.getClickedPos(), context.isSecondaryUseActive(), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()) {
            ShipSystem.openEditor(serverPlayer, "");
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
