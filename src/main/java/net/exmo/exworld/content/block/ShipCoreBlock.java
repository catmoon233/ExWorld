package net.exmo.exworld.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Origin marker captured into a hull. Has no world tick. */
public final class ShipCoreBlock extends Block {
    public static final MapCodec<ShipCoreBlock> CODEC = simpleCodec(ShipCoreBlock::new);
    public ShipCoreBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
}
