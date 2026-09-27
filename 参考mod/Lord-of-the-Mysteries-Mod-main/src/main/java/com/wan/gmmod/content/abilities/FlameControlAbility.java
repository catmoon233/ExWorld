package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 「操纵火焰」——魔术师 / 无面人共用的主动能力（参数化火焰伤害倍率）。
 * <p>
 * 30 米内点燃视线所指的生物或方块。无面人升级版火焰伤害 +30%。
 * 秘偶大师解锁「火焰操纵强化」后，点燃地面时凭空召唤 3×3 焰流。
 */
public class FlameControlAbility extends Ability {
    /** 操纵火焰射程（米） */
    private static final double RANGE = 30.0;
    /** 直接火焰伤害基础值 */
    private static final float BASE_DAMAGE = 2.0F;

    private final float damageMultiplier;

    public FlameControlAbility(String path, float damageMultiplier) {
        // 消耗 6 灵性，冷却 60 刻（3 秒），主动能力
        super(GuimiMod.id(path), 6, 60, true);
        this.damageMultiplier = damageMultiplier;
    }

    @Override
    public void onActivate(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // 优先点燃视线内的生物
        LivingEntity target = AbilityTargeting.pickLivingEntity(player, RANGE);
        if (target != null) {
            target.igniteForSeconds(6);
            target.hurt(level.damageSources().inFire(), BASE_DAMAGE * damageMultiplier);
            level.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    20, 0.3, 0.4, 0.3, 0.03);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }
        // 否则在方块落点点火；秘偶大师的「火焰操纵强化」扩大为 3×3 焰流
        BlockHitResult hit = AbilityTargeting.pickBlock(player, RANGE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        boolean mastery = SkillManager.isUnlocked(player, GuimiMod.id("flame_mastery"));
        int radius = mastery ? 1 : 0;
        BlockPos base = hit.getBlockPos().relative(hit.getDirection());
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = base.offset(dx, 0, dz);
                if (level.isEmptyBlock(pos) && BaseFireBlock.canBePlacedAt(level, pos, hit.getDirection())) {
                    level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
                }
            }
        }
        level.playSound(null, base, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
