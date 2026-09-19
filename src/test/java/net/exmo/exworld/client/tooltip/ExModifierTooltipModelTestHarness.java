package net.exmo.exworld.client.tooltip;

import net.exmo.exmodifier.api.AppliedModifierView;
import net.exmo.exmodifier.core.data.AttributeSpec;
import net.exmo.exmodifier.core.data.ModifierEntryDefinition;
import net.exmo.exmodifier.core.data.SuitDefinition;
import net.exmo.exmodifier.core.data.SuitLevel;
import net.exmo.exmodifier.core.registry.ExModifierCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ExModifierTooltipModelTestHarness {
    public static void main(String[] args) {
        ResourceLocation sharp = ResourceLocation.fromNamespaceAndPath("exmodifier", "sharp");
        ResourceLocation blades = ResourceLocation.fromNamespaceAndPath("exmodifier", "blades");
        ModifierEntryDefinition definition = new ModifierEntryDefinition(
                List.of(), List.of(), 1.0F, 5, true, false,
                List.of(new AttributeSpec(ResourceLocation.parse("minecraft:generic.attack_damage"), 1.0D, 0.5D,
                        AttributeModifier.Operation.ADD_VALUE, Optional.empty())),
                List.of(), List.of(), Optional.of("tooltip.exmodifier.entry.sharp"));
        SuitDefinition suit = new SuitDefinition(
                List.of(sharp),
                List.of(new SuitLevel(1, ResourceLocation.parse("exmodifier:tick"), List.of(
                        new AttributeSpec(ResourceLocation.parse("minecraft:generic.attack_speed"), 0.05D, 0.0D,
                                AttributeModifier.Operation.ADD_VALUE, Optional.empty())))));
        ExModifierCatalog.bind(new ExModifierCatalog(
                Map.of(sharp, definition), Map.of(), Map.of(), Map.of(), Map.of(blades, suit),
                Map.of(), Map.of(), Map.of(), Map.of()));
        try {
            AppliedModifierView view = new AppliedModifierView(sharp, 2, Optional.empty(), false);
            List<Chip> chips = ExModifierTooltip.chips(List.of(view), ExModifierCatalog.current(), key -> {
                if ("tooltip.exmodifier.entry.sharp".equals(key)) return "Sharp";
                return key;
            });
            if (chips.size() != 1) throw new AssertionError("expected one affix chip");
            if (!"Sharp Lv.2".equals(chips.getFirst().label())) {
                throw new AssertionError("chip should include translated name and level, got " + chips.getFirst().label());
            }
            List<ExModifierTooltip.SuitSection> sections = ExModifierTooltip.suits(
                    List.of(view), List.of(view), ExModifierCatalog.current(),
                    key -> "tooltip.exmodifier.suit.blades".equals(key) ? "Blades" : key);
            ExModifierTooltip.SlotSection emptySlots = ExModifierTooltip.slotSection(
                    null, ExModifierCatalog.current(), key -> key);
            if (emptySlots.total() != 0) throw new AssertionError("null stack must have no slot section");
            if (sections.size() != 1) throw new AssertionError("expected blades suit section");
            ExModifierTooltip.SuitSection section = sections.getFirst();
            if (!"Blades".equals(section.name())) throw new AssertionError("suit name");
            if (section.owned() != 1 || section.required() != 1) {
                throw new AssertionError("suit count should be 1/1, got " + section.owned() + "/" + section.required());
            }
            if (section.bonuses().isEmpty() || !section.bonuses().getFirst().active()) {
                throw new AssertionError("1-piece bonus should be active");
            }
            if (!section.bonuses().getFirst().text().contains("+0.05")) {
                throw new AssertionError("bonus text should include +0.05, got " + section.bonuses().getFirst().text());
            }
        } finally {
            ExModifierCatalog.bind(ExModifierCatalog.EMPTY);
        }
        System.out.println("ExModifier tooltip model tests passed");
    }
}
