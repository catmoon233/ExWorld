package com.wan.gmmod.content.world;

import com.wan.gmmod.common.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 「好运圃」世界生成特征。
 * <p>
 * 以一株「好运之花」为中心，其下生成「好运之花生长的泥土」；四叶草以好运之花
 * 为中心向四周（曼哈顿距离 ≤ 3 的环形区域）生长；每株四叶草有小概率（15%）
 * 变异为「银色四叶草」。仅生成于泥土系方块（草方块 / 泥土等）之上。
 */
public class GuimiLuckyGardenFeature extends Feature<NoneFeatureConfiguration> {

    /** 银色四叶草的变异概率（百分比）。 */
    private static final int SILVER_CLOVER_CHANCE = 15;

    /** 四叶草环绕生长的最大曼哈顿距离。 */
    private static final int RING_SPREAD = 3;

    public GuimiLuckyGardenFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        BlockPos ground = origin.below();
        BlockState groundState = level.getBlockState(ground);
        if (!groundState.is(BlockTags.DIRT) && !groundState.is(Blocks.FARMLAND)) {
            return false;
        }
        if (!level.getBlockState(origin).isAir() || !level.getBlockState(origin.above()).isAir()) {
            return false;
        }

        // 中心：好运之花泥土 + 好运之花
        level.setBlock(ground, ModBlocks.LUCKY_FLOWER_SOIL.get().defaultBlockState(), 3);
        level.setBlock(origin, ModBlocks.LUCKY_FLOWER.get().defaultBlockState(), 3);

        // 四周：四叶草环形生长（含小概率银色四叶草）
        int planted = 0;
        for (BlockPos target : BlockPos.betweenClosed(
                origin.offset(-RING_SPREAD, 0, -RING_SPREAD),
                origin.offset(RING_SPREAD, 0, RING_SPREAD))) {
            if (target.equals(origin)) {
                continue;
            }
            int dx = target.getX() - origin.getX();
            int dz = target.getZ() - origin.getZ();
            if (Math.abs(dx) + Math.abs(dz) > RING_SPREAD) {
                continue;
            }
            BlockPos below = target.below();
            BlockState belowState = level.getBlockState(below);
            if ((!belowState.is(BlockTags.DIRT) && !belowState.is(Blocks.FARMLAND))
                    || !level.getBlockState(target).isAir()) {
                continue;
            }
            if (random.nextInt(100) < SILVER_CLOVER_CHANCE) {
                level.setBlock(target, ModBlocks.SILVER_FOUR_LEAF_CLOVER.get().defaultBlockState(), 3);
            } else {
                level.setBlock(target, ModBlocks.FOUR_LEAF_CLOVER.get().defaultBlockState(), 3);
            }
            planted++;
        }
        return true;
    }
}
