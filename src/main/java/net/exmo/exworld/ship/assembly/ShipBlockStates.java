package net.exmo.exworld.ship.assembly;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** String keys for hull palette. Properties stay in the key so doors and levers round-trip. */
public final class ShipBlockStates {
    private ShipBlockStates() {}

    public static String serialize(BlockState state) {
        return BlockStateParser.serialize(state);
    }

    public static BlockState parse(String key) {
        if (key == null || key.isBlank()) return Blocks.AIR.defaultBlockState();
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), key, false).blockState();
        } catch (Exception ignored) {
            return Blocks.AIR.defaultBlockState();
        }
    }
}
