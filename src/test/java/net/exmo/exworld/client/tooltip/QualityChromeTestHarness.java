package net.exmo.exworld.client.tooltip;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

import java.util.Optional;

public final class QualityChromeTestHarness {
    public static void main(String[] args) {
        ResourceLocation rare = ResourceLocation.fromNamespaceAndPath("exmodifier", "rare");
        if (QualityChrome.accent(Rarity.COMMON, Optional.of(rare)) != RarityPalette.qualityColor(rare)) {
            throw new AssertionError("quality accent must match tooltip quality colour");
        }
        if (QualityChrome.accent(Rarity.EPIC, Optional.empty()) != RarityPalette.color(Rarity.EPIC)) {
            throw new AssertionError("missing quality falls back to vanilla rarity colour");
        }
        int fill = QualityChrome.slotFill(Rarity.COMMON, Optional.of(rare));
        int expected = RarityPalette.theme(Rarity.COMMON, Optional.of(rare)).slotFill();
        if (fill != expected) throw new AssertionError("slot fill must use tooltip theme");
        System.out.println("Quality chrome tests passed");
    }
}
