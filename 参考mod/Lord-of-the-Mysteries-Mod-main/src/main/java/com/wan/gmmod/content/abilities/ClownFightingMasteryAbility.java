package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * 小丑「格斗精通」——序列 8 被动能力。
 * <p>
 * 常驻 力量I + 速度I；同时大幅减少摔落伤害
 * （摔落减免在 {@link com.wan.gmmod.common.event.AbilityEventSubscriber} 中处理）。
 */
public class ClownFightingMasteryAbility extends Ability {

    public ClownFightingMasteryAbility() {
        super(GuimiMod.id("fighting_mastery"));
    }

    @Override
    public void onPassiveTick(Player player) {
        if (player.level().isClientSide || player.tickCount % 40 != 0) {
            return;
        }
        // 周期性刷新，效果时长略大于刷新间隔以保证常驻不闪烁
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 90, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 90, 0, false, false, true));
    }
}
