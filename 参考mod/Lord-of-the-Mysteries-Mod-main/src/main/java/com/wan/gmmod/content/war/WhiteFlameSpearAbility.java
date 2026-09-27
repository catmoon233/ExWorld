package com.wan.gmmod.content.war;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.entities.FlameSpearEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 「炽白之枪」——纵火家（战争之红途径 · 序列 7）主动。
 * <p>
 * 投掷一柄炽白火焰长枪：伤害 9，直线穿透，命中方块时引导附近火球
 * 向命中点靠拢（见 {@link FlameSpearEntity}）。冷却 15 秒。
 */
public class WhiteFlameSpearAbility extends Ability {

    public WhiteFlameSpearAbility() {
        super(GuimiMod.id("white_flame_spear"), 8, 15 * 20, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        FlameSpearEntity spear = new FlameSpearEntity(level, sp);
        spear.setPos(sp.getX(), sp.getEyeY() - 0.2, sp.getZ());
        spear.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 2.0F, 0.5F);
        level.addFreshEntity(spear);
        level.playSound(null, sp.blockPosition(),
                SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.4F);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.white_flame_spear.done"), true);
    }
}
