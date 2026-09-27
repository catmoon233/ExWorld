package com.wan.gmmod.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 通用「十字形」草本植物方块（金薄荷 / 夜香草 / 龙血草 / 毒堇）。
 * <p>
 * 使用十字（cross）模型渲染，可种植于泥土 / 草方块 / 耕地之上，破坏后按对应战利品表掉落。
 * 与原版花类似：无碰撞、瞬间破坏、可被活塞摧毁、带 XZ 随机偏移。
 */
public class GuimiPlantBlock extends BushBlock {
    public static final MapCodec<GuimiPlantBlock> CODEC = simpleCodec(GuimiPlantBlock::new);

    // 略小于整格的十字碰撞盒（仅用于射线检测，无实体碰撞）
    private static final VoxelShape SHAPE = net.minecraft.world.level.block.Block.box(
            2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public GuimiPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 可种植于泥土系方块与耕地之上。 */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(Blocks.FARMLAND);
    }
}
