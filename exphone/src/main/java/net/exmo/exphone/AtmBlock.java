package net.exmo.exphone;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Account terminal. Withdraw and deposit paper gold; admins can credit themselves. */
public final class AtmBlock extends Block {
    public static final MapCodec<AtmBlock> CODEC = simpleCodec(AtmBlock::new);

    public AtmBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            PhoneSystem.send(server, AtmActions.openJson(server, ""));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
