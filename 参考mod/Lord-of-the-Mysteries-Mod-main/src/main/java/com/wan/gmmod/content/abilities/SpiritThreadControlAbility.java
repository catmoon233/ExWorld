package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * 秘偶大师「灵体之线操控」——序列 5 主动能力。
 * <p>
 * 对 20 米内视线所指目标释放灵体之线：目标获得缓慢、挖掘疲劳、移动减速，
 * 持续 5 秒后进入「秘偶化」状态（见 {@link com.wan.gmmod.content.marionette.MarionetteManager}）。
 * 冷却 30 秒。半神以下几乎无法挣脱。
 */
public class SpiritThreadControlAbility extends Ability {
    /** 操控射程（米） */
    private static final double RANGE = 20.0;
    /** 操控 → 秘偶化所需时长（刻，5 秒） */
    public static final int CONTROL_TICKS = 100;

    public SpiritThreadControlAbility() {
        // 消耗 15 灵性，冷却 600 刻（30 秒），主动能力
        super(GuimiMod.id("spirit_thread_control"), 15, 600, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity picked = AbilityTargeting.pickLivingEntity(player, RANGE);
        if (!(picked instanceof Mob mob)) {
            player.displayClientMessage(
                    Component.translatable("ability.guimi_mod.spirit_thread_control.no_target"), true);
            return;
        }
        // 线程持续期间：缓慢 + 挖掘疲劳 + 发光标记
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CONTROL_TICKS, 2, false, true));
        mob.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, CONTROL_TICKS, 2, false, true));
        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, CONTROL_TICKS, 0, false, false));
        player.setData(ModAttachments.THREAD_TARGET_UUID, mob.getUUID().toString());
        player.setData(ModAttachments.THREAD_CONTROL_END, level.getGameTime() + CONTROL_TICKS);
        level.playSound(null, mob.blockPosition(),
                SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.6F, 1.6F);
        player.displayClientMessage(Component.translatable(
                "ability.guimi_mod.spirit_thread_control.start", mob.getDisplayName()), true);
    }
}
