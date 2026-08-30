package net.exmo.exworld.world.generation;

import net.exmo.exworld.world.model.WorldBiome;
import net.exmo.exworld.world.model.WorldTile;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** Adds biome-specific surface identity after vanilla terrain shape generation, once for each new chunk. */
public final class WorldTerrainAdapter {
    private WorldTerrainAdapter() {}

    public static void apply(ChunkAccess chunk, WorldTile tile, long seed, int minimumY) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = minX + localX;
                int worldZ = minZ + localZ;
                int surfaceY = findSolidSurface(chunk, cursor, localX, localZ, worldX, worldZ, minimumY);
                if (surfaceY <= minimumY) continue;
                double detail = unit(seed, worldX, worldZ, 19);
                Block top = surface(tile.biome(), detail);
                replaceNatural(chunk, cursor.set(worldX, surfaceY, worldZ), top.defaultBlockState());
                Block under = underSurface(tile.biome(), detail);
                for (int depth = 1; depth <= 2 && surfaceY - depth > minimumY; depth++) {
                    replaceNatural(chunk, cursor.set(worldX, surfaceY - depth, worldZ), under.defaultBlockState());
                }
                placeDetail(chunk, cursor, tile.biome(), seed, worldX, surfaceY + 1, worldZ);
            }
        }
    }

    private static int findSolidSurface(ChunkAccess chunk, BlockPos.MutableBlockPos cursor, int localX, int localZ,
                                        int worldX, int worldZ, int minimumY) {
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ) - 1;
        for (int scan = 0; scan < 12 && y > minimumY; scan++, y--) {
            BlockState state = chunk.getBlockState(cursor.set(worldX, y, worldZ));
            if (!state.isAir() && state.getFluidState().isEmpty() && !state.is(BlockTags.LEAVES)) return y;
        }
        return minimumY;
    }

    private static Block surface(WorldBiome biome, double detail) {
        return switch (biome) {
            case PRAIRIE, FOREST, FLOWER_FIELDS -> Blocks.GRASS_BLOCK;
            case SWAMP -> detail < 0.72 ? Blocks.MUD : Blocks.GRASS_BLOCK;
            case DESERT -> Blocks.SAND;
            case BADLANDS -> Blocks.RED_SAND;
            case TAIGA -> detail < 0.68 ? Blocks.PODZOL : Blocks.COARSE_DIRT;
            case HIGHLANDS -> detail < 0.62 ? Blocks.STONE : Blocks.GRAVEL;
        };
    }

    private static Block underSurface(WorldBiome biome, double detail) {
        return switch (biome) {
            case DESERT -> Blocks.SANDSTONE;
            case BADLANDS -> detail < 0.5 ? Blocks.TERRACOTTA : Blocks.RED_SANDSTONE;
            case HIGHLANDS -> Blocks.STONE;
            case SWAMP -> Blocks.MUD;
            default -> Blocks.DIRT;
        };
    }

    private static void replaceNatural(ChunkAccess chunk, BlockPos pos, BlockState replacement) {
        BlockState current = chunk.getBlockState(pos);
        if (current.is(BlockTags.DIRT) || current.is(BlockTags.BASE_STONE_OVERWORLD)
                || current.is(Blocks.SAND) || current.is(Blocks.RED_SAND) || current.is(Blocks.GRAVEL)
                || current.is(Blocks.CLAY) || current.is(Blocks.MUD) || current.is(Blocks.SNOW_BLOCK)) {
            chunk.setBlockState(pos, replacement, false);
        }
    }

    private static void placeDetail(ChunkAccess chunk, BlockPos.MutableBlockPos cursor, WorldBiome biome,
                                    long seed, int x, int y, int z) {
        if (!chunk.getBlockState(cursor.set(x, y, z)).isAir()) return;
        double chance = unit(seed, x, z, 73);
        Block detail = switch (biome) {
            case PRAIRIE -> chance < 0.075 ? Blocks.SHORT_GRASS : null;
            case FOREST -> chance < 0.035 ? Blocks.FERN : null;
            case SWAMP -> chance < 0.022 ? Blocks.BROWN_MUSHROOM : null;
            case DESERT, BADLANDS -> chance < 0.018 ? Blocks.DEAD_BUSH : null;
            case TAIGA -> chance < 0.028 ? Blocks.FERN : null;
            case HIGHLANDS -> null;
            case FLOWER_FIELDS -> chance < 0.065 ? Blocks.POPPY : chance < 0.14 ? Blocks.DANDELION : null;
        };
        if (detail != null) chunk.setBlockState(cursor, detail.defaultBlockState(), false);
    }

    private static double unit(long seed, int x, int z, int salt) {
        long value = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ salt;
        value ^= value >>> 30; value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27; value *= 0x94D049BB133111EBL; value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }
}
