package net.exmo.exworld.equipment;

import net.exmo.exmodifier.api.ExModifierApi;
import net.exmo.exmodifier.core.component.AppliedModifier;
import net.exmo.exmodifier.core.component.AppliedModifiers;
import net.exmo.exmodifier.core.data.AttributeSpec;
import net.exmo.exmodifier.core.data.ModifierEntryDefinition;
import net.exmo.exmodifier.core.registry.ExModifierCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ExModifierBridgeTestHarness {
    public static void main(String[] args) {
        ResourceLocation sharp = ResourceLocation.fromNamespaceAndPath("exmodifier", "sharp");
        AppliedModifiers applied = new AppliedModifiers(List.of(
                new AppliedModifier(sharp, 2, Optional.empty(), false)));
        ModifierEntryDefinition definition = new ModifierEntryDefinition(
                List.of(), List.of(), 1.0F, 5, true, false,
                List.of(new AttributeSpec(ResourceLocation.parse("minecraft:generic.attack_damage"), 1.0D, 0.5D,
                        AttributeModifier.Operation.ADD_VALUE, Optional.empty())),
                List.of(), List.of(), Optional.empty());
        ExModifierCatalog.bind(new ExModifierCatalog(
                Map.of(sharp, definition), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()));
        if (ExModifierApi.modifiersOn(applied).isEmpty()) {
            throw new AssertionError("item with modifier component queries non-empty");
        }
        if (ExModifierApi.attributesOn(applied).isEmpty()) {
            throw new AssertionError("modifier attributes should be readable");
        }
        ExModifierCatalog.bind(ExModifierCatalog.EMPTY);
        System.out.println("ExModifier bridge tests passed");
    }
}
