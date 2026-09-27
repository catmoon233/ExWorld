package com.wan.gmmod.common.block;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.common.block.entity.AltarBlockEntity;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.registry.ModBlockEntities;
import com.wan.gmmod.content.altar.AltarRecipe;
import com.wan.gmmod.content.meditation.MeditationManager;
import com.wan.gmmod.content.spiritwall.SpiritWallManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 祭台方块：仪式系统核心。
 * <p>
 * 功能：
 * - 右键放置材料物品（存储到 BlockEntity 内部槽位）
 * - 材料齐全后空手右键合成魔药（仪式条件：处于灵性之墙内且周围摆放至少 3 根蜡烛）
 * - 潜行 + 空手右键：选择“冥想”，进入 / 退出冥想姿态
 */
public class AltarBlock extends BaseEntityBlock {

    public static final MapCodec<AltarBlock> CODEC = simpleCodec(AltarBlock::new);

    // 祭台碰撞体积（比完整方块略小）
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 12, 15);

    /** 仪式蜡烛检测半径（以祭台为中心） */
    private static final int CANDLE_RADIUS = 4;
    /** 仪式所需蜡烛数量 */
    private static final int CANDLES_REQUIRED = 3;

    public AltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AltarBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AltarBlockEntity altar)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 空手右键：尝试合成或取出物品；潜行时选择“冥想”
        if (stack.isEmpty()) {
            // 潜行 + 空手右键：进入 / 退出冥想
            if (player.isShiftKeyDown()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    MeditationManager.toggle(serverPlayer);
                }
                return ItemInteractionResult.SUCCESS;
            }

            // 材料已可匹配配方时，先校验仪式条件（灵性之墙内 + 蜡烛x3）
            if (altar.hasMatchingRecipe() && !checkRitualConditions(level, pos, player)) {
                return ItemInteractionResult.SUCCESS;
            }

            // 灵性消耗校验：配方要求灵性时，先扣除施法玩家的灵性再合成
            AltarRecipe recipe = altar.matchingRecipe();
            if (recipe != null && recipe.spiritCost() > 0) {
                if (!(player instanceof ServerPlayer sp)) {
                    return ItemInteractionResult.SUCCESS;
                }
                int spirit = SpiritualityManager.get(sp);
                if (spirit < recipe.spiritCost()) {
                    player.displayClientMessage(
                            Component.translatable("message.guimi_mod.altar_need_spirit", recipe.spiritCost()), true);
                    return ItemInteractionResult.SUCCESS;
                }
                SpiritualityManager.set(sp, spirit - recipe.spiritCost());
            }

            // 尝试合成
            ItemStack result = altar.tryCraft();
            if (!result.isEmpty()) {
                // 合成成功，弹出产物
                ItemEntity itemEntity = new ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, result);
                itemEntity.setDeltaMovement(0, 0.15, 0);
                level.addFreshEntity(itemEntity);
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.displayClientMessage(Component.translatable("message.guimi_mod.altar_crafted"), true);
            } else if (altar.hasItems()) {
                // 取出最后放入的物品
                ItemStack removed = altar.removeLastItem();
                if (!removed.isEmpty()) {
                    player.addItem(removed);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }

        // 持物品右键：放置到祭台
        if (altar.addItem(stack.copy())) {
            if (!player.isCreative()) {
                stack.shrink(stack.getCount());
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(
                    Component.translatable("message.guimi_mod.altar_item_placed"), true);
            return ItemInteractionResult.SUCCESS;
        }

        // 祭台已满
        player.displayClientMessage(Component.translatable("message.guimi_mod.altar_full"), true);
        return ItemInteractionResult.FAIL;
    }

    /**
     * 校验祭台仪式条件：
     * - 祭台必须处于活跃的灵性之墙内
     * - 祭台周围需摆放至少 3 根蜡烛（原版任意颜色蜡烛）
     */
    private static boolean checkRitualConditions(Level level, BlockPos pos, Player player) {
        if (!SpiritWallManager.isInsideAnyWall(pos)) {
            player.displayClientMessage(
                    Component.translatable("message.guimi_mod.altar_need_wall"), true);
            return false;
        }

        int candles = 0;
        for (BlockPos p : BlockPos.betweenClosed(
                pos.offset(-CANDLE_RADIUS, -CANDLE_RADIUS, -CANDLE_RADIUS),
                pos.offset(CANDLE_RADIUS, CANDLE_RADIUS, CANDLE_RADIUS))) {
            if (level.getBlockState(p).is(BlockTags.CANDLES)) {
                candles++;
                if (candles >= CANDLES_REQUIRED) {
                    return true;
                }
            }
        }

        player.displayClientMessage(
                Component.translatable("message.guimi_mod.altar_need_candles", CANDLES_REQUIRED), true);
        return false;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AltarBlockEntity altar) {
                altar.dropAllItems(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
