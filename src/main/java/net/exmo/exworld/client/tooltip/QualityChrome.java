package net.exmo.exworld.client.tooltip;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

import java.util.Optional;

/** Slot chrome uses the same accent as the item tooltip. */
public final class QualityChrome {
    private QualityChrome() {}

    public static int accent(Rarity rarity, Optional<ResourceLocation> qualityId) {
        if (qualityId != null && qualityId.isPresent()) return RarityPalette.qualityColor(qualityId.get());
        return RarityPalette.color(rarity);
    }

    public static int slotFill(Rarity rarity, Optional<ResourceLocation> qualityId) {
        return RarityPalette.theme(rarity, qualityId == null ? Optional.empty() : qualityId).slotFill();
    }
}
