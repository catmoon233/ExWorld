package com.wan.gmmod.content.war;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.abilities.Ability;
import net.minecraft.world.entity.player.Player;

/**
 * 「火焰抗性」——纵火家（战争之红途径 · 序列 7）被动。
 * <p>
 * 火焰伤害减免 80%（判定在 WarAbilityEventSubscriber），免疫燃烧效果
 * （每刻扑灭身上的火），但爆炸冲击波仍造成正常伤害。
 */
public class PyroFireResistanceAbility extends Ability {

    public PyroFireResistanceAbility() {
        super(GuimiMod.id("pyro_fire_resistance"));
    }

    @Override
    public void onPassiveTick(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        // 免疫燃烧：随时扑灭身上的火
        if (player.isOnFire()) {
            player.clearFire();
        }
    }
}
