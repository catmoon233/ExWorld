package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 「蛛丝操控」——欢愉魔女（魔女途径 · 序列 6）主动。
 * <p>
 * 发射无形蛛丝缠绕 20 米内视线所指目标：移动速度 -60%（缓慢 IV）、
 * 攻击 / 挖掘速度 -30%（挖掘疲劳 I），持续 8 秒。
 * 潜行触发时改为拉扯自己向目标快速位移（蜘蛛侠摆荡）。
 * 消耗 8 灵性，冷却 15 秒。
 */
public class SpiderSilkAbility extends Ability {
    private static final double RANGE = 20.0;
    /** 缠绕持续时间（刻，8 秒） */
    private static final int DURATION = 8 * 20;

    public SpiderSilkAbility() {
        super(GuimiMod.id("spider_silk"), 8, 15 * 20, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = AbilityTargeting.pickLivingEntity(player, RANGE);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.guimi_mod.spider_silk.no_target"), true);
            return;
        }
        // 蛛丝轨迹粒子
        Vec3 from = player.getEyePosition();
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 step = to.subtract(from).normalize().scale(0.5);
        Vec3 cursor = from;
        for (int i = 0; i < from.distanceTo(to) * 2 && i < 80; i++) {
            level.sendParticles(ParticleTypes.END_ROD, cursor.x, cursor.y, cursor.z, 1, 0, 0, 0, 0);
            cursor = cursor.add(step);
        }
        if (player.isShiftKeyDown()) {
            // 蜘蛛侠摆荡：拉自己冲向目标
            Vec3 pull = to.subtract(player.position()).normalize().scale(2.2).add(0, 0.4, 0);
            player.setDeltaMovement(pull);
            player.hurtMarked = true; // 强制同步速度到客户端
            level.playSound(null, player.blockPosition(),
                    SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0F, 0.6F);
            return;
        }
        // 缠绕减速：移速 -60%（缓慢 IV，每级 -15%），攻速 / 挖掘 -30%（挖掘疲劳 I）
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION, 3, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, DURATION, 0, false, true));
        level.sendParticles(ParticleTypes.ITEM_COBWEB,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                20, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, target.blockPosition(),
                SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.8F, 1.4F);
    }
}
