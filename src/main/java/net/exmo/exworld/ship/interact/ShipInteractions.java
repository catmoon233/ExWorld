package net.exmo.exworld.ship.interact;

import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.ship.assembly.ShipBlockStates;
import net.exmo.exworld.ship.entity.ShipEntity;
import net.exmo.exworld.ship.model.ShipOccupancy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiFunction;

/** Whitelist-only mounted interactions. No redstone graph and no furnace ticking. */
public final class ShipInteractions {
    private ShipInteractions() {}

    public static boolean handle(ServerPlayer player, ShipEntity ship, int x, int y, int z) {
        String key = ship.hull().blockAt(x, y, z).orElse("");
        if (key.isBlank()) return false;
        BlockState state = ShipBlockStates.parse(key);
        if (state.isAir()) return false;
        if (state.getBlock() == ExWorldContent.SHIP_HELM.get()) {
            ship.beginDriving(player, x, y, z);
            return true;
        }
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof FenceGateBlock) {
            toggle(ship, x, y, z, state, BlockStateProperties.OPEN);
            if (state.getBlock() instanceof DoorBlock && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                int otherY = y + (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF).ordinal() == 0 ? 1 : -1);
                ship.hull().blockAt(x, otherY, z).ifPresent(otherKey -> {
                    BlockState other = ShipBlockStates.parse(otherKey);
                    if (other.getBlock() instanceof DoorBlock) toggle(ship, x, otherY, z, other, BlockStateProperties.OPEN);
                });
            }
            return true;
        }
        if (state.getBlock() instanceof LeverBlock) {
            toggle(ship, x, y, z, state, BlockStateProperties.POWERED);
            return true;
        }
        if (state.getBlock() instanceof ButtonBlock) {
            if (state.hasProperty(BlockStateProperties.POWERED) && !state.getValue(BlockStateProperties.POWERED)) {
                ship.setBlock(x, y, z, ShipBlockStates.serialize(state.setValue(BlockStateProperties.POWERED, true)));
                ship.pressButton(ShipOccupancy.pack(x, y, z), key);
            }
            return true;
        }
        if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock
                || state.getBlock() instanceof ShulkerBoxBlock) {
            int packed = ShipOccupancy.pack(x, y, z);
            MountedInventory inventory = new MountedInventory(27, ship.hull().containerAt(packed), player.serverLevel(), ship,
                    slots -> ship.replaceContainer(packed, slots));
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.threeRows(id, inv, inventory),
                    Component.translatable("container.chest")));
            return true;
        }
        if (state.getBlock() instanceof CraftingTableBlock) {
            return open(player, "container.crafting", (id, inv) -> new CraftingMenu(id, inv, ContainerLevelAccess.NULL));
        }
        if (state.getBlock() instanceof StonecutterBlock) {
            return open(player, "container.stonecutter", (id, inv) -> new StonecutterMenu(id, inv, ContainerLevelAccess.NULL));
        }
        if (state.getBlock() instanceof CartographyTableBlock) {
            return open(player, "container.cartography_table", (id, inv) -> new CartographyTableMenu(id, inv, ContainerLevelAccess.NULL));
        }
        if (state.getBlock() instanceof LoomBlock) {
            return open(player, "container.loom", (id, inv) -> new LoomMenu(id, inv, ContainerLevelAccess.NULL));
        }
        if (state.getBlock() instanceof GrindstoneBlock) {
            return open(player, "container.grindstone", (id, inv) -> new GrindstoneMenu(id, inv, ContainerLevelAccess.NULL));
        }
        if (state.getBlock() instanceof SmithingTableBlock) {
            return open(player, "container.upgrade", (id, inv) -> new SmithingMenu(id, inv, ContainerLevelAccess.NULL));
        }
        return false;
    }

    public static boolean inReach(ServerPlayer player, ShipEntity ship, int x, int y, int z) {
        if (!ship.getBoundingBox().inflate(8).contains(player.getEyePosition())) return false;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1);
        Vec3 local = ship.toLocal(eye);
        return ShipRaycast.trace(local.x, local.y, local.z, look.x, look.y, look.z, 8, ship.hull()::occupied)
                .filter(hit -> hit.x() == x && hit.y() == y && hit.z() == z)
                .isPresent();
    }

    private static boolean open(ServerPlayer player, String title, BiFunction<Integer, Inventory, AbstractContainerMenu> factory) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> factory.apply(id, inv), Component.translatable(title)));
        return true;
    }

    private static void toggle(ShipEntity ship, int x, int y, int z, BlockState state,
                               net.minecraft.world.level.block.state.properties.BooleanProperty property) {
        if (!state.hasProperty(property)) return;
        ship.setBlock(x, y, z, ShipBlockStates.serialize(state.setValue(property, !state.getValue(property))));
    }
}
