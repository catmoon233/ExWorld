package net.exmo.exworld.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Helm voxel. Driving is handled after 实体化, not as a world block menu. */
public final class ShipHelmBlock extends Block {
    public static final MapCodec<ShipHelmBlock> CODEC = simpleCodec(ShipHelmBlock::new);
    public ShipHelmBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
}
