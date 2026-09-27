package com.wan.gmmod.content.war;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.CooldownData;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityTargeting;
import com.wan.gmmod.content.entities.FireRavenEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 「火鸦术」——纵火家（战争之红途径 · 序列 7）主动。
 * <p>
 * 召唤 5 只火鸦扇形飞向 15 米内的视线目标，每只伤害 2 并点燃，
 * 飞行中自动追踪目标。冷却 12 秒（选中目标才结算）。
 */
public class FireRavenAbility extends Ability {
    private static final double RANGE = 15.0;
    private static final int COUNT = 5;
    private static final int COST = 8;
    private static final int COOLDOWN = 12 * 20;

    public FireRavenAbility() {
        super(GuimiMod.id("fire_raven"), 0, 0, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = AbilityTargeting.pickLivingEntity(sp, RANGE);
        if (target == null) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.fire_raven.no_target"), true);
            return;
        }
        int spirituality = SpiritualityManager.get(sp);
        if (spirituality < COST) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.skill.no_spirituality"), true);
            return;
        }
        SpiritualityManager.set(sp, spirituality - COST);

        // 扇形释放 5 只火鸦
        for (int i = 0; i < COUNT; i++) {
            FireRavenEntity raven = new FireRavenEntity(level, sp, target);
            raven.setPos(sp.getX(), sp.getEyeY(), sp.getZ());
            float yawOffset = (i - COUNT / 2) * 12.0F;
            Vec3 dir = Vec3.directionFromRotation(sp.getXRot() - 10.0F, sp.getYRot() + yawOffset);
            raven.setDeltaMovement(dir.scale(0.6));
            level.addFreshEntity(raven);
        }
        level.playSound(null, sp.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.9F, 1.6F);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.fire_raven.done",
                target.getDisplayName()), true);

        long now = level.getGameTime();
        CooldownData cd = sp.getData(ModAttachments.SKILL_COOLDOWNS)
                .with(getId(), now + COOLDOWN, now);
        sp.setData(ModAttachments.SKILL_COOLDOWNS, cd);
    }
}
