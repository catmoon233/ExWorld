package net.exmo.lotm.phone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Two-click box tool for phone-map places and roads. */
public final class MapWandItem extends Item {
    private static final String[] KINDS = {"地点", "道路"};

    public MapWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer server) mark(server, context.getItemInHand(), context.getClickedPos());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) cycle(player, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (player instanceof ServerPlayer server) {
            if (!PhoneMap.editor(server)) {
                player.displayClientMessage(Component.literal("需要创造模式或管理员"), true);
                return InteractionResultHolder.fail(stack);
            }
            PhoneSystem.send(server, PhoneMap.get(server).editorJson(stack));
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = data(stack);
        tooltip.add(Component.literal(KINDS[Math.floorMod(tag.getInt("kind"), KINDS.length)]));
        tooltip.add(Component.literal("对方块点两下框选，潜行切换地点/道路"));
    }

    private static void cycle(Player player, ItemStack stack) {
        CompoundTag tag = data(stack);
        int next = Math.floorMod(tag.getInt("kind") + 1, KINDS.length);
        tag.putInt("kind", next);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        player.displayClientMessage(Component.literal("地图笔：" + KINDS[next]), true);
    }

    private static void mark(ServerPlayer player, ItemStack stack, BlockPos pos) {
        if (!PhoneMap.editor(player)) {
            player.displayClientMessage(Component.literal("需要创造模式或管理员"), true);
            return;
        }
        CompoundTag tag = data(stack);
        String dim = player.serverLevel().dimension().location().toString();
        boolean restart = !tag.getBoolean("hasA") || tag.getBoolean("hasB") || !dim.equals(tag.getString("dim"));
        if (restart) {
            tag.putBoolean("hasA", true);
            tag.putBoolean("hasB", false);
            tag.putInt("ax", pos.getX());
            tag.putInt("ay", pos.getY());
            tag.putInt("az", pos.getZ());
            tag.putString("dim", dim);
            player.displayClientMessage(Component.literal("第一点 " + pos.getX() + " " + pos.getY() + " " + pos.getZ()), true);
        } else {
            tag.putBoolean("hasB", true);
            tag.putInt("bx", pos.getX());
            tag.putInt("by", pos.getY());
            tag.putInt("bz", pos.getZ());
            player.displayClientMessage(Component.literal("已框选，右键空气打开编辑"), true);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
