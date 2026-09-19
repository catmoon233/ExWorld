package net.exmo.exworld.ship.view;

import net.exmo.exworld.ship.assembly.ShipBlockStates;
import net.exmo.exworld.ship.model.ShipHull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** Read-only hull view for ray tests and mesh baking. Not a full Level. */
public final class ShipView implements BlockGetter {
    private final ShipHull hull;

    public ShipView(ShipHull hull) { this.hull = hull; }

    @Override public BlockEntity getBlockEntity(BlockPos pos) { return null; }

    @Override public BlockState getBlockState(BlockPos pos) {
        return hull.blockAt(pos.getX(), pos.getY(), pos.getZ()).map(ShipBlockStates::parse).orElse(Blocks.AIR.defaultBlockState());
    }

    @Override public FluidState getFluidState(BlockPos pos) { return Fluids.EMPTY.defaultFluidState(); }

    @Override public int getHeight() { return Math.max(1, hull.sizeY()); }

    @Override public int getMinBuildHeight() { return 0; }

    @Override public int getMaxLightLevel() { return 15; }

    public ShipHull hull() { return hull; }

    public boolean solid(BlockPos pos) { return hull.occupied(pos.getX(), pos.getY(), pos.getZ()); }

    public int getSectionCount() { return 1; }
}
