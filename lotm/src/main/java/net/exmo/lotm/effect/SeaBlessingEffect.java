package net.exmo.lotm.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Marker plus full water-movement efficiency. Breathing and night vision are applied beside it. */
public final class SeaBlessingEffect extends MobEffect {
    public SeaBlessingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3AA0FF);
        addAttributeModifier(Attributes.WATER_MOVEMENT_EFFICIENCY,
                ResourceLocation.fromNamespaceAndPath("lotm", "sea_blessing_water"),
                1.0,
                AttributeModifier.Operation.ADD_VALUE);
    }
}
