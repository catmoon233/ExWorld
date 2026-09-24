package net.exmo.lotm.sequence;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Flat or multiplicative attribute bonus granted by one sequence. */
public record AttributeGrant(ResourceLocation attribute, double amount, AttributeModifier.Operation operation) {
    public AttributeGrant {
        if (attribute == null) throw new IllegalArgumentException("attribute");
        if (operation == null) operation = AttributeModifier.Operation.ADD_VALUE;
    }

    public static AttributeGrant add(ResourceLocation attribute, double amount) {
        return new AttributeGrant(attribute, amount, AttributeModifier.Operation.ADD_VALUE);
    }
}
