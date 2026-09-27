package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 魔术师「制造幻觉」——序列 7 主动能力。
 * <p>
 * 在视线落点（30 米内）生成粒子云并播放假声音，
 * 吸引附近 16 米内生物的注意力：清除仇恨并使其走向幻觉位置。
 */
public class IllusionAbility extends Ability {
    /** 幻觉投放射程（米） */
    private static final double RANGE = 30.0;
    /** 吸引生物的半径（米） */
    private static final double LURE_RADIUS = 16.0;

    public IllusionAbility() {
        // 消耗 5 灵性，冷却 200 刻（10 秒），主动能力
        super(GuimiMod.id("illusion"), 5, 200, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockHitResult hit = AbilityTargeting.pickBlock(player, RANGE);
        Vec3 pos = hit.getLocation();
        // 粒子云 + 假声音
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 40, 0.8, 0.8, 0.8, 0.02);
        level.playSound(null, BlockPos.containing(pos),
                SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.5F, 1.0F);
        // 吸引周围生物注意力
        AABB box = new AABB(pos, pos).inflate(LURE_RADIUS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            mob.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.0);
        }
    }
}
