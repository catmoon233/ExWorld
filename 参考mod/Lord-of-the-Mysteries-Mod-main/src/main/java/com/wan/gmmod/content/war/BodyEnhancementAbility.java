package com.wan.gmmod.content.war;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.abilities.Ability;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 「身体强化」——猎人（战争之红途径 · 序列 9）被动。
 * <p>
 * 常驻力量 I + 速度 I，最大生命值 +4（2 颗心）。
 */
public class BodyEnhancementAbility extends Ability {
    private static final ResourceLocation HEALTH_MODIFIER_ID = GuimiMod.id("body_enhancement_health");
    private static final double HEALTH_BONUS = 4.0;

    public BodyEnhancementAbility() {
        super(GuimiMod.id("body_enhancement"));
    }

    @Override
    public void onPassiveTick(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && !maxHealth.hasModifier(HEALTH_MODIFIER_ID)) {
            maxHealth.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER_ID,
                    HEALTH_BONUS, AttributeModifier.Operation.ADD_VALUE));
        }
        // 常驻力量 I + 速度 I（每 tick 刷新，保持不闪烁）
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 220, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 220, 0, true, false));
    }

    @Override
    public void onDeactivate(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(HEALTH_MODIFIER_ID);
        }
    }
}
