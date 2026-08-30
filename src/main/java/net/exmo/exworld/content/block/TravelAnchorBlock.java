package net.exmo.exworld.content.block;

import com.mojang.serialization.MapCodec;
import net.exmo.exworld.world.WorldSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class TravelAnchorBlock extends Block {
    public static final MapCodec<TravelAnchorBlock> CODEC = simpleCodec(TravelAnchorBlock::new);

    public TravelAnchorBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            WorldSystem.useAnchor(serverPlayer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
