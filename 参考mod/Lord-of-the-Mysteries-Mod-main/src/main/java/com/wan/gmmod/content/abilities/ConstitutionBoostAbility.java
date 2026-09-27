package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 「体质增强」——教唆者（魔女途径 · 序列 8）被动。
 * <p>
 * 最大生命值 +4（2 颗心），常驻抗性提升 I。
 */
public class ConstitutionBoostAbility extends Ability {
    private static final ResourceLocation HEALTH_MODIFIER_ID = GuimiMod.id("constitution_boost_health");
    private static final double HEALTH_BONUS = 4.0;

    public ConstitutionBoostAbility() {
        super(GuimiMod.id("constitution_boost"));
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
        // 常驻抗性提升 I（每 tick 刷新，保持不闪烁）
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 220, 0, true, false));
    }

    @Override
    public void onDeactivate(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(HEALTH_MODIFIER_ID);
        }
    }
}
