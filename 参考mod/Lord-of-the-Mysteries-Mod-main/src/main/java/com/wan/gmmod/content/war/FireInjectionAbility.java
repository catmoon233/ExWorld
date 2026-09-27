package com.wan.gmmod.content.war;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.abilities.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 「注火」——纵火家（战争之红途径 · 序列 7）主动。
 * <p>
 * 触发后开启 3 秒「武装窗口」：期间的下一次近战命中会将火焰注入目标体内，
 * 3 秒后在目标体内爆炸，额外伤害 6（命中判定与延时爆炸在
 * WarAbilityEventSubscriber / WarPathwayManager）。冷却 10 秒。
 */
public class FireInjectionAbility extends Ability {
    /** 武装窗口：3 秒。 */
    public static final int ARM_WINDOW = 3 * 20;

    public FireInjectionAbility() {
        super(GuimiMod.id("fire_injection"), 5, 10 * 20, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        sp.setData(ModAttachments.FIRE_INJECTION_ARM_END, level.getGameTime() + ARM_WINDOW);
        level.playSound(null, sp.blockPosition(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5F, 1.8F);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.fire_injection.armed"), true);
    }
}
