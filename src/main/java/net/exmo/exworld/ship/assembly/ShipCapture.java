package net.exmo.exworld.ship.assembly;

import net.exmo.exworld.ship.model.ShipBlock;
import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Collects world voxels into a 船体结构. Air and fluids are the flood boundary. */
public final class ShipCapture {
    public record Result(ShipHull hull, BlockPos origin) {}

    private ShipCapture() {}

    public static boolean capturable(BlockState state) {
        return !state.isAir() && state.getFluidState().getType() == Fluids.EMPTY;
    }

    public static Result box(ServerLevel level, BlockPos a, BlockPos b) {
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        List<ShipBlock> blocks = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            add(level, pos.immutable(), blocks);
            if (blocks.size() > ShipHull.MAX_BLOCKS) throw new IllegalArgumentException("capture exceeds " + ShipHull.MAX_BLOCKS + " blocks");
        }
        return finish(blocks);
    }

    public static Result flood(ServerLevel level, BlockPos start) {
        if (!capturable(level.getBlockState(start))) throw new IllegalArgumentException("start block cannot be captured");
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        List<ShipBlock> blocks = new ArrayList<>();
        queue.add(start.immutable());
        seen.add(start.immutable());
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            add(level, pos, blocks);
            if (blocks.size() > ShipHull.MAX_BLOCKS) throw new IllegalArgumentException("capture exceeds " + ShipHull.MAX_BLOCKS + " blocks");
            for (BlockPos next : new BlockPos[]{pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()}) {
                BlockPos immutable = next.immutable();
                if (seen.add(immutable) && capturable(level.getBlockState(immutable))) queue.add(immutable);
            }
        }
        return finish(blocks);
    }

    public static Result positions(ServerLevel level, Collection<BlockPos> positions) {
        List<ShipBlock> blocks = new ArrayList<>();
        for (BlockPos pos : positions) {
            add(level, pos, blocks);
            if (blocks.size() > ShipHull.MAX_BLOCKS) throw new IllegalArgumentException("capture exceeds " + ShipHull.MAX_BLOCKS + " blocks");
        }
        return finish(blocks);
    }

    private static Result finish(List<ShipBlock> blocks) {
        if (blocks.isEmpty()) throw new IllegalArgumentException("selection is empty");
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (ShipBlock block : blocks) {
            minX = Math.min(minX, block.x());
            minY = Math.min(minY, block.y());
            minZ = Math.min(minZ, block.z());
        }
        return new Result(ShipHull.of(blocks), new BlockPos(minX, minY, minZ));
    }

    private static void add(ServerLevel level, BlockPos pos, List<ShipBlock> blocks) {
        BlockState state = level.getBlockState(pos);
        if (!capturable(state)) return;
        blocks.add(new ShipBlock(pos.getX(), pos.getY(), pos.getZ(), ShipBlockStates.serialize(state), slots(level, pos)));
    }

    private static List<ShipSlot> slots(ServerLevel level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof Container container)) return List.of();
        List<ShipSlot> slots = new ArrayList<>();
        for (int i = 0; i < Math.min(256, container.getContainerSize()); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            String extra = "";
            Tag saved = stack.save(level.registryAccess());
            if (saved != null) extra = saved.toString();
            slots.add(new ShipSlot(i, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), extra));
        }
        return slots;
    }
}
