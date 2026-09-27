package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * 魔术师「水下呼吸」——序列 7 被动能力。
 * <p>
 * 身处水下时自动获得水下呼吸 I。
 */
public class WaterBreathingAbility extends Ability {

    public WaterBreathingAbility() {
        super(GuimiMod.id("water_breathing"));
    }

    @Override
    public void onPassiveTick(Player player) {
        if (player.level().isClientSide || player.tickCount % 40 != 0) {
            return;
        }
        if (player.isUnderWater()) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 100, 0, false, false, true));
        }
    }
}
