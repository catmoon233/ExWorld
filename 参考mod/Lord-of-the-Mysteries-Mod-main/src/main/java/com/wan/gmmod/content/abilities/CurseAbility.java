package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 「诅咒」——欢愉魔女（魔女途径 · 序列 6）主动。
 * <p>
 * 需手持镜子锁定 15 米内视线所指目标，消耗 50 灵性，
 * 施加随机诅咒：失明 / 缓慢 / 虚弱 / 中毒，持续 30 秒。冷却 120 秒。
 * 灵性消耗与冷却由技能框架结算（构造器已声明）。
 */
public class CurseAbility extends Ability {
    private static final double RANGE = 15.0;
    private static final int DURATION = 30 * 20;

    @SuppressWarnings("unchecked")
    private static final Holder<MobEffect>[] CURSES = new Holder[] {
            MobEffects.BLINDNESS, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.POISON
    };

    public CurseAbility() {
        super(GuimiMod.id("curse"), 50, 120 * 20, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // 需手持镜子
        if (!player.getMainHandItem().is(ModItems.MIRROR.get())
                && !player.getOffhandItem().is(ModItems.MIRROR.get())) {
            player.displayClientMessage(Component.translatable("message.guimi_mod.curse.need_mirror"), true);
            return;
        }
        LivingEntity target = AbilityTargeting.pickLivingEntity(player, RANGE);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.guimi_mod.curse.no_target"), true);
            return;
        }
        Holder<MobEffect> curse = CURSES[player.getRandom().nextInt(CURSES.length)];
        target.addEffect(new MobEffectInstance(curse, DURATION, 0, false, true));
        level.sendParticles(ParticleTypes.WITCH,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                30, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, target.blockPosition(),
                SoundEvents.WITCH_CELEBRATE, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.displayClientMessage(Component.translatable("message.guimi_mod.curse.done",
                target.getDisplayName()), true);
    }
}
