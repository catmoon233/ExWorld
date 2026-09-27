package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * 占卜家「危机占卜」——序列 9 主动能力示例。
 * <p>
 * 触发后消耗灵性，为周围 16 格内的敌对生物附加发光效果，帮助玩家感知潜伏的危险。
 * 作为技能栏系统的可触发范例，玩家可仿此新增更多主动能力。
 */
public class SeerDivinationAbility extends Ability {
    private static final double RADIUS = 16.0;
    private static final int GLOW_TICKS = 200;

    public SeerDivinationAbility() {
        // 消耗 10 灵性，冷却 600 刻（30 秒），主动能力
        super(GuimiMod.id("seer_divination"), 10, 600, true);
    }

    @Override
    public void onActivate(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        AABB box = player.getBoundingBox().inflate(RADIUS);
        for (Monster monster : player.level().getEntitiesOfClass(Monster.class, box)) {
            monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false));
        }
        player.displayClientMessage(Component.translatable("ability.guimi_mod.seer_divination.activate"), true);
    }
}
