package net.exmo.exworld.npc.item;

import net.exmo.exworld.npc.NpcSystem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Editor wand. Sneak-use cycles the placement mode; a block click writes that mode into the selected document. */
public final class NpcWandItem extends Item {
    public static final String[] MODES = {"select", "home", "waypoint", "post", "undo"};

    public NpcWandItem(Properties properties) { super(properties); }

    public static int mode(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.floorMod(tag.getInt("mode"), MODES.length);
    }

    public static String documentId(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("doc");
    }

    public static void remember(ItemStack stack, String documentId) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString("doc", documentId == null ? "" : documentId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) return net.minecraft.world.InteractionResultHolder.success(stack);
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            int next = Math.floorMod(tag.getInt("mode") + 1, MODES.length);
            tag.putInt("mode", next);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            player.displayClientMessage(Component.translatable("npc.exworld.wand_mode", Component.translatable("npc.exworld.mode." + MODES[next])), true);
            return net.minecraft.world.InteractionResultHolder.consume(stack);
        }
        if (level.isClientSide()) return net.minecraft.world.InteractionResultHolder.success(stack);
        if (player instanceof ServerPlayer serverPlayer) {
            if (documentId(stack).isBlank()) serverPlayer.displayClientMessage(Component.translatable("npc.exworld.wand_pick"), true);
            else NpcSystem.openBlueprint(serverPlayer, documentId(stack));
        }
        return net.minecraft.world.InteractionResultHolder.consume(stack);
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) NpcSystem.applyWand(serverPlayer, context.getItemInHand(), context.getClickedPos());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) return NpcSystem.wandOnEntity(serverPlayer, stack, entity);
        return InteractionResult.PASS;
    }
    @Override
     public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("npc.exworld.mode." + MODES[mode(stack)]));
        String id = documentId(stack);
        if (!id.isBlank()) tooltip.add(Component.literal(id));
        tooltip.add(Component.translatable("npc.exworld.wand_hint"));
    }

}
