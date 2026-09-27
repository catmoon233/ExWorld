package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * 无面人「变形」——序列 6 主动能力。
 * <p>
 * 千变万化地融入环境：清除周围所有 NPC 对自己的仇恨（被观众途径克制）。
 */
public class DisguiseAbility extends Ability {
    /** 清除仇恨的范围（米） */
    private static final double RANGE = 32.0;

    public DisguiseAbility() {
        // 消耗 8 灵性，冷却 300 刻（15 秒），主动能力
        super(GuimiMod.id("disguise"), 8, 300, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        int cleared = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RANGE))) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
                mob.setLastHurtByMob(null);
                cleared++;
            }
        }
        level.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 1.2F);
        player.displayClientMessage(
                Component.translatable("ability.guimi_mod.disguise.activate", cleared), true);
    }
}
