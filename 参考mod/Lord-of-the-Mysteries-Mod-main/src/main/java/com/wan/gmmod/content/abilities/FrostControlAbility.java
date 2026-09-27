package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 「操控冰霜」——女巫（魔女途径 · 序列 7）主动。
 * <p>
 * 优先冻结 8 米内视线所指目标：3 秒无法移动（极限缓慢）+ 后续减速 5 秒；
 * 视线上无目标时在地面落点制造 3×3 霜冰面（滑行效果）。
 * 消耗 10 灵性，冷却 10 秒。
 */
public class FrostControlAbility extends Ability {
    private static final double RANGE = 8.0;
    /** 定身时长（刻，3 秒） */
    private static final int FREEZE_TICKS = 3 * 20;
    /** 后续减速时长（刻，5 秒） */
    private static final int SLOW_TICKS = 5 * 20;

    public FrostControlAbility() {
        super(GuimiMod.id("frost_control"), 10, 200, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // 优先冻结视线内目标
        LivingEntity target = AbilityTargeting.pickLivingEntity(player, RANGE);
        if (target != null) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_TICKS, 255, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_TICKS + SLOW_TICKS, 1, false, true));
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), FREEZE_TICKS + 140));
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    30, 0.3, 0.5, 0.3, 0.04);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.6F);
            return;
        }
        // 否则在地面落点铺 3×3 霜冰面
        BlockHitResult hit = AbilityTargeting.pickBlock(player, RANGE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos base = hit.getBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = base.offset(dx, 0, dz);
                if (level.getBlockState(pos).is(Blocks.WATER)
                        || level.getBlockState(pos).isSolidRender(level, pos)) {
                    // 水面结冰 / 实体方块表面覆霜冰
                    BlockPos icePos = level.getBlockState(pos).is(Blocks.WATER) ? pos : pos.above();
                    if (level.getBlockState(icePos).is(Blocks.WATER) || level.isEmptyBlock(icePos)) {
                        level.setBlockAndUpdate(icePos, Blocks.FROSTED_ICE.defaultBlockState());
                    }
                }
            }
        }
        level.playSound(null, base, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.0F, 1.2F);
    }
}
