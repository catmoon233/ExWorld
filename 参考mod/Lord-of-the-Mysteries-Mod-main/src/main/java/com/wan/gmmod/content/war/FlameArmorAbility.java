package com.wan.gmmod.content.war;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.abilities.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 「火焰护甲」——纵火家（战争之红途径 · 序列 7）主动。
 * <p>
 * 体表燃起火焰，持续 15 秒：近战攻击者受 2 点火焰反伤，
 * 冰冻 / 毒气（中毒类魔法）伤害减半（判定在 WarAbilityEventSubscriber）。冷却 30 秒。
 */
public class FlameArmorAbility extends Ability {
    /** 持续 15 秒。 */
    public static final int DURATION = 15 * 20;

    public FlameArmorAbility() {
        super(GuimiMod.id("flame_armor"), 10, 30 * 20, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        sp.setData(ModAttachments.FLAME_ARMOR_END, level.getGameTime() + DURATION);
        level.playSound(null, sp.blockPosition(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.9F);
        level.sendParticles(ParticleTypes.FLAME,
                sp.getX(), sp.getY() + 1.0, sp.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.flame_armor.on"), true);
    }
}
