package com.wan.gmmod.content.effects;

import com.wan.gmmod.GuimiMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 「激怒」——挑衅者（战争之红途径 · 序列 8）「挑衅」施加的状态。
 * <p>
 * 被激怒的生物强制锁定挑衅者为攻击目标（目标锁定由 {@code CharmManager} 驱动），
 * 移动速度 +20%，但攻击命中率 -30%（失准判定在 {@code WarAbilityEventSubscriber}）。
 */
public class EnragedEffect extends MobEffect {
    public EnragedEffect() {
        super(MobEffectCategory.HARMFUL, 0xC62B1F);
        // 移动速度 +20%
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                GuimiMod.id("enraged_speed"), 0.20,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
