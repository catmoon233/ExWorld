package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 「火焰跳跃」——魔术师 / 无面人共用的主动能力（参数化射程）。
 * <p>
 * 若视线落点附近存在火焰，则瞬移过去；可与「操纵火焰」联动：先点火，再跳跃。
 * 魔术师射程 30 米，无面人升级版 45 米。
 */
public class FlameJumpAbility extends Ability {
    /** 落点附近搜索火焰的半径（格） */
    private static final int FIRE_SEARCH_RADIUS = 2;

    private final double range;

    public FlameJumpAbility(String path, double range) {
        // 消耗 8 灵性，冷却 100 刻（5 秒），主动能力
        super(GuimiMod.id(path), 8, 100, true);
        this.range = range;
    }

    @Override
    public void onActivate(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockHitResult hit = AbilityTargeting.pickBlock(player, range);
        BlockPos center = hit.getBlockPos();
        // 在落点附近寻找火焰（火 / 灵魂火 / 点燃的营火）
        BlockPos firePos = null;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-FIRE_SEARCH_RADIUS, -FIRE_SEARCH_RADIUS, -FIRE_SEARCH_RADIUS),
                center.offset(FIRE_SEARCH_RADIUS, FIRE_SEARCH_RADIUS, FIRE_SEARCH_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.FIRE)
                    || (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT))) {
                firePos = pos.immutable();
                break;
            }
        }
        if (firePos == null) {
            player.displayClientMessage(Component.translatable("ability.guimi_mod.flame_jump.no_fire"), true);
            return;
        }
        level.sendParticles(ParticleTypes.FLAME,
                player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.3, 0.6, 0.3, 0.02);
        double x = firePos.getX() + 0.5;
        double y = firePos.getY() + 0.1;
        double z = firePos.getZ() + 0.5;
        if (player instanceof ServerPlayer sp) {
            sp.teleportTo(level, x, y, z, sp.getYRot(), sp.getXRot());
        } else {
            player.teleportTo(x, y, z);
        }
        player.fallDistance = 0.0F;
        player.clearFire();
        level.sendParticles(ParticleTypes.FLAME, x, y + 1.0, z, 30, 0.3, 0.6, 0.3, 0.02);
        level.playSound(null, firePos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
    }
}
