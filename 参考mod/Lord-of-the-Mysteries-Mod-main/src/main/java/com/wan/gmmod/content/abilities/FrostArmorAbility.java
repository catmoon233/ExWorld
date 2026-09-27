package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 「冰霜护甲」——欢愉魔女（魔女途径 · 序列 6）主动（冰霜强化的一部分）。
 * <p>
 * 自身覆盖冰霜护甲：吸收 30% 伤害，持续 20 秒。冷却 90 秒，消耗 15 灵性。
 * 减伤判定见 {@code WitchAbilityEventSubscriber#onFrostArmorDamage}。
 */
public class FrostArmorAbility extends Ability {
    /** 护甲持续时间（刻，20 秒） */
    public static final int DURATION = 20 * 20;
    /** 伤害吸收比例 */
    public static final float ABSORB_RATIO = 0.30F;

    public FrostArmorAbility() {
        super(GuimiMod.id("frost_armor"), 15, 90 * 20, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        player.setData(ModAttachments.FROST_ARMOR_END, level.getGameTime() + DURATION);
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                player.getX(), player.getY() + 1.0, player.getZ(),
                50, 0.4, 0.9, 0.4, 0.04);
        level.playSound(null, player.blockPosition(),
                SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.displayClientMessage(Component.translatable("message.guimi_mod.frost_armor.start"), true);
    }

    @Override
    public void onDeactivate(Player player) {
        player.setData(ModAttachments.FROST_ARMOR_END, 0L);
    }
}
