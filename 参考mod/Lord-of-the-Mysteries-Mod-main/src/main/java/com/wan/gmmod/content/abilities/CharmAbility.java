package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.charm.CharmManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * 「魅惑」——教唆者（魔女途径 · 序列 8）主动。
 * <p>
 * 对 10 米内视线所指目标施放：敌对生物变为中立，中立生物变为跟随，
 * 持续 30 秒。对玩家无效。冷却 60 秒，消耗 15 灵性。
 * 生物 AI 控制由 {@link CharmManager} 每刻驱动。
 */
public class CharmAbility extends Ability {
    private static final double RANGE = 10.0;
    private static final int DURATION = 30 * 20;

    public CharmAbility() {
        super(GuimiMod.id("charm"), 15, 60 * 20, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        LivingEntity target = AbilityTargeting.pickLivingEntity(player, RANGE);
        if (!(target instanceof Mob mob)) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.charm.no_target"), true);
            return;
        }
        if (mob instanceof Enemy) {
            CharmManager.calm(mob, sp, DURATION);
            sp.displayClientMessage(Component.translatable("message.guimi_mod.charm.calmed",
                    mob.getDisplayName()), true);
        } else {
            CharmManager.follow(mob, sp, DURATION);
            sp.displayClientMessage(Component.translatable("message.guimi_mod.charm.following",
                    mob.getDisplayName()), true);
        }
        sp.level().playSound(null, mob.blockPosition(),
                SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.PLAYERS, 1.0F, 1.4F);
    }
}
