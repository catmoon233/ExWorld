package com.wan.gmmod.content.war;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.abilities.Ability;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 「体质强化」——挑衅者（战争之红途径 · 序列 8）被动。
 * <p>
 * 自然生命恢复速度 +50%（每 4 秒额外恢复 0.5 点，需饱食度充足），
 * 最大生命值额外 +2（1 颗心，与猎人「身体强化」的 +4 叠加）。
 */
public class WarConstitutionAbility extends Ability {
    private static final ResourceLocation HEALTH_MODIFIER_ID = GuimiMod.id("war_constitution_health");
    private static final double HEALTH_BONUS = 2.0;
    /** 原版满饱食自然回血约每 4 秒 1 点，+50% 即每 4 秒额外 0.5 点。 */
    private static final int REGEN_INTERVAL = 80;

    public WarConstitutionAbility() {
        super(GuimiMod.id("war_constitution"));
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
        // 自然恢复 +50%：饱食度足以触发原版自然回血时，周期性追加恢复
        if (player.level().getGameTime() % REGEN_INTERVAL == 0
                && player.getFoodData().getFoodLevel() >= 18
                && player.getHealth() < player.getMaxHealth()) {
            player.heal(0.5F);
        }
    }

    @Override
    public void onDeactivate(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(HEALTH_MODIFIER_ID);
        }
    }
}
