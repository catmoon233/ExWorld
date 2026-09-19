package net.exmo.exworld.client.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

import java.util.List;
import java.util.Optional;

/** View-model consumed by layout and the custom tooltip renderer. */
public record TooltipModel(
        Component title,
        List<NameTag> nameTags,
        String rarityLabel,
        Rarity rarity,
        Optional<ResourceLocation> qualityId,
        List<Chip> chips,
        ExModifierTooltip.SlotSection slots,
        List<ExModifierTooltip.SuitSection> suits,
        List<Component> bodyLines
) {
    public TooltipModel {
        title = title == null ? Component.empty() : title;
        nameTags = List.copyOf(nameTags == null ? List.of() : nameTags);
        rarityLabel = rarityLabel == null ? "" : rarityLabel;
        rarity = rarity == null ? Rarity.COMMON : rarity;
        qualityId = qualityId == null ? Optional.empty() : qualityId;
        chips = List.copyOf(chips == null ? List.of() : chips);
        slots = slots == null ? new ExModifierTooltip.SlotSection(0, 0, List.of()) : slots;
        suits = List.copyOf(suits == null ? List.of() : suits);
        bodyLines = List.copyOf(bodyLines == null ? List.of() : bodyLines);
    }
}
