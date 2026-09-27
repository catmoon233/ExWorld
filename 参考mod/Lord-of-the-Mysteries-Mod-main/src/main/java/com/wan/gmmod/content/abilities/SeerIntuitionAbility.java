package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class SeerIntuitionAbility extends Ability {
    public SeerIntuitionAbility() {
        super(GuimiMod.id("seer_intuition"));
    }

    @Override
    public void onPassiveTick(Player player) {
        // 每 tick 刷新夜视效果，确保持续
        if (!player.level().isClientSide) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false));
        }
    }
}