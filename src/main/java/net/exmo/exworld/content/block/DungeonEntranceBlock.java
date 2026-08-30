package net.exmo.exworld.content.block;

import com.mojang.serialization.MapCodec;
import net.exmo.exworld.dungeon.DungeonSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.exmo.exworld.content.ExWorldContent;

public final class DungeonEntranceBlock extends BaseEntityBlock {
    public static final MapCodec<DungeonEntranceBlock> CODEC = simpleCodec(DungeonEntranceBlock::new);
    public DungeonEntranceBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DungeonEntranceBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity entity=level.getBlockEntity(pos);
            String dungeonId=entity instanceof DungeonEntranceBlockEntity entrance?entrance.dungeonId():DungeonSystem.DEFAULT_DUNGEON;
            DungeonSystem.enter(serverPlayer, dungeonId, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
