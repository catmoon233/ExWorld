package com.wan.gmmod.common.item;

import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.content.entities.PaperFigurineEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 纸人物品：右键地面放置一个纸人实体（{@link PaperFigurineEntity}）。
 * <p>
 * 纸人是「纸人替身」能力的交换锚点，被替身消耗或被打碎后需重新放置。
 */
public class PaperFigurineItem extends Item {

    public PaperFigurineItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        PaperFigurineEntity figurine = ModEntities.PAPER_FIGURINE.get().create(level);
        if (figurine == null) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        float yaw = player == null ? 0.0F : player.getYRot() + 180.0F;
        figurine.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
        level.addFreshEntity(figurine);
        level.playSound(null, pos, SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 1.2F);

        ItemStack stack = context.getItemInHand();
        if (player == null || !player.isCreative()) {
            stack.shrink(1);
        }
        if (level instanceof ServerLevel) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }
}
