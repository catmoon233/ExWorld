package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.marionette.MarionetteManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * 秘偶大师「秘偶战斗」——序列 5 主动能力。
 * <p>
 * 命令秘偶攻击视线所指目标；此外玩家处于潜行姿态时，
 * 秘偶会自动索敌、代替玩家战斗（见 {@link MarionetteManager}）。
 * 秘偶保留原始实体数据，战斗时使用其原有的能力。
 */
public class MarionetteCombatAbility extends Ability {
    /** 指定攻击目标的射程（米） */
    private static final double RANGE = 24.0;

    public MarionetteCombatAbility() {
        // 消耗 3 灵性，冷却 40 刻（2 秒），主动能力
        super(GuimiMod.id("marionette_combat"), 3, 40, true);
    }

    @Override
    public void onActivate(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        Mob marionette = MarionetteManager.getMarionette(player);
        if (marionette == null) {
            player.displayClientMessage(
                    Component.translatable("ability.guimi_mod.marionette_combat.no_marionette"), true);
            return;
        }
        LivingEntity target = AbilityTargeting.pickLivingEntity(player, RANGE);
        if (target != null && target != marionette) {
            marionette.setTarget(target);
            player.displayClientMessage(Component.translatable(
                    "ability.guimi_mod.marionette_combat.attack", target.getDisplayName()), true);
        } else {
            player.displayClientMessage(
                    Component.translatable("ability.guimi_mod.marionette_combat.hint"), true);
        }
    }
}
