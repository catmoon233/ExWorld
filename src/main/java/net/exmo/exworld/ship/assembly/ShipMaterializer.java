package net.exmo.exworld.ship.assembly;

import net.exmo.exworld.ship.entity.ShipEntity;
import net.exmo.exworld.ship.model.PartSelection;
import net.exmo.exworld.ship.model.ShipBlock;
import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipSlot;
import net.exmo.exworld.ship.model.ShipTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Lifts a captured hull into a 飞船实例 or writes it back as world blocks. No physics. */
public final class ShipMaterializer {
    private ShipMaterializer() {}

    public static ShipEntity spawn(ServerLevel level, ShipTemplate template, Vec3 origin) {
        ShipEntity entity = ShipEntity.create(level, template.hull(), template.id(), template.selection(), template.maxSpeed());
        entity.moveTo(origin.x, origin.y, origin.z, 0, 0);
        level.addFreshEntity(entity);
        return entity;
    }

    public static ShipEntity lift(ServerLevel level, ShipHull hull, String templateId, PartSelection selection, double speed, Vec3 origin) {
        ShipEntity entity = ShipEntity.create(level, hull, templateId, selection, speed);
        entity.moveTo(origin.x, origin.y, origin.z, 0, 0);
        level.addFreshEntity(entity);
        return entity;
    }

    public static void clearWorld(ServerLevel level, BlockPos origin, ShipHull hull) {
        for (ShipBlock block : hull.blocks()) {
            BlockPos pos = origin.offset(block.x(), block.y(), block.z());
            level.removeBlockEntity(pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    public static void disassemble(ServerLevel level, ShipEntity entity) {
        BlockPos origin = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
        ShipHull hull = entity.hull();
        for (ShipBlock block : hull.blocks()) {
            BlockPos pos = origin.offset(block.x(), block.y(), block.z());
            if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).liquid()) {
                throw new IllegalStateException("cannot disassemble: destination occupied at " + pos);
            }
        }
        for (ShipBlock block : hull.blocks()) {
            BlockPos pos = origin.offset(block.x(), block.y(), block.z());
            BlockState state = ShipBlockStates.parse(block.block());
            level.setBlock(pos, state, 3);
            restore(level, pos, block.slots());
        }
        entity.discard();
    }

    public static boolean destinationClear(ServerLevel level, BlockPos origin, ShipHull hull) {
        for (ShipBlock block : hull.blocks()) {
            BlockPos pos = origin.offset(block.x(), block.y(), block.z());
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.liquid()) return false;
        }
        return true;
    }

    private static void restore(ServerLevel level, BlockPos pos, java.util.List<ShipSlot> slots) {
        if (slots.isEmpty()) return;
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof Container container)) return;
        for (ShipSlot slot : slots) {
            if (slot.index() >= container.getContainerSize()) continue;
            container.setItem(slot.index(), stack(level, slot));
        }
        entity.setChanged();
    }

    public static ItemStack stack(ServerLevel level, ShipSlot slot) {
        if (slot.extra() != null && !slot.extra().isBlank()) {
            try {
                CompoundTag tag = TagParser.parseTag(slot.extra());
                ItemStack parsed = ItemStack.parse(level.registryAccess(), tag).orElse(ItemStack.EMPTY);
                if (!parsed.isEmpty()) return parsed;
            } catch (Exception ignored) {
            }
        }
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(slot.itemId()));
        return new ItemStack(item, slot.count());
    }
}
