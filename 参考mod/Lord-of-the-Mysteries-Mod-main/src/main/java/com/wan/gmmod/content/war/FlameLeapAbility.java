package com.wan.gmmod.content.war;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.CooldownData;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.entities.FlameOrbEntity;
import com.wan.gmmod.content.entities.FlameTrapEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 「火焰跃迁」——阴谋家（战争之红途径 · 序列 6）主动。
 * <p>
 * 向 30 米内最近的己方火焰（火球 / 火焰陷阱 / 燃烧之墙）位置瞬移，
 * 抵达后引爆原位置火焰。冷却 8 秒（成功跃迁才结算，构造器填 0 手动写入）。
 */
public class FlameLeapAbility extends Ability {
    private static final double RANGE = 30.0;
    private static final int COST = 8;
    private static final int COOLDOWN = 8 * 20;

    public FlameLeapAbility() {
        super(GuimiMod.id("flame_leap"), 0, 0, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        // 候选一：己方火球 / 火焰陷阱中最近者
        List<Projectile> flames = level.getEntitiesOfClass(Projectile.class,
                sp.getBoundingBox().inflate(RANGE),
                e -> (e instanceof FlameOrbEntity || e instanceof FlameTrapEntity)
                        && e.isAlive() && e.getOwner() == sp);
        Projectile nearestFlame = null;
        double flameDist = RANGE * RANGE;
        for (Projectile flame : flames) {
            double d = flame.distanceToSqr(sp);
            if (d < flameDist) {
                flameDist = d;
                nearestFlame = flame;
            }
        }
        // 候选二：己方燃烧之墙中最近者
        FireWallManager.Wall wall = FireWallManager.nearestWall(level, sp.position(), sp.getUUID(), RANGE);
        double wallDist = wall == null ? Double.MAX_VALUE : wall.center().distanceToSqr(sp.position());

        if (nearestFlame == null && wall == null) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.flame_leap.no_flame"), true);
            return;
        }
        int spirituality = SpiritualityManager.get(sp);
        if (spirituality < COST) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.skill.no_spirituality"), true);
            return;
        }
        SpiritualityManager.set(sp, spirituality - COST);

        // 出发点火焰残影
        level.sendParticles(ParticleTypes.LARGE_SMOKE, sp.getX(), sp.getY() + 1.0, sp.getZ(),
                20, 0.3, 0.6, 0.3, 0.02);

        Vec3 dest;
        if (nearestFlame != null && flameDist <= wallDist) {
            // 瞬移到火焰弹射物位置并引爆
            dest = nearestFlame.position();
            sp.teleportTo(dest.x, dest.y, dest.z);
            if (nearestFlame instanceof FlameTrapEntity trap) {
                trap.detonate();
            } else {
                FlameOrbEntity.explode(level, dest, sp, 5.0F, 2.0F, false);
                nearestFlame.discard();
            }
        } else {
            // 瞬移到火墙中心并引爆整圈火墙
            dest = wall.center();
            sp.teleportTo(dest.x, dest.y, dest.z);
            FireWallManager.removeWall(wall);
            FlameOrbEntity.explode(level, dest, sp, 5.0F, (float) FireWallManager.RADIUS, false);
        }
        level.sendParticles(ParticleTypes.FLAME, dest.x, dest.y + 1.0, dest.z,
                30, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, sp.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.flame_leap.done"), true);

        long now = level.getGameTime();
        CooldownData cd = sp.getData(ModAttachments.SKILL_COOLDOWNS)
                .with(getId(), now + COOLDOWN, now);
        sp.setData(ModAttachments.SKILL_COOLDOWNS, cd);
    }
}
