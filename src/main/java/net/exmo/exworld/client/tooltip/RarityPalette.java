package net.exmo.exworld.client.tooltip;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

import java.util.Optional;

/**
 * ColorTooltips-style rarity colours: frame and background follow vanilla rarity,
 * with ExModifier quality replacing the frame tint when present.
 */
public final class RarityPalette {
    public static final int COMMON = 0xFFFFFFFF;
    public static final int UNCOMMON = 0xFFFFFF44;
    public static final int RARE = 0xFF55FFFF;
    public static final int EPIC = 0xFFFF55FF;
    public static final int LEGENDARY = 0xFFFFAA00;

    private RarityPalette() {}

    public static int color(Rarity rarity) {
        if (rarity == null) return COMMON;
        return switch (rarity) {
            case UNCOMMON -> UNCOMMON;
            case RARE -> RARE;
            case EPIC -> EPIC;
            default -> COMMON;
        };
    }

    public static int qualityColor(ResourceLocation qualityId) {
        if (qualityId == null) return COMMON;
        return switch (qualityId.getPath()) {
            case "uncommon" -> UNCOMMON;
            case "rare" -> RARE;
            case "epic" -> EPIC;
            case "legendary", "mythic" -> LEGENDARY;
            default -> COMMON;
        };
    }

    public static TooltipTheme theme(Rarity rarity, Optional<ResourceLocation> qualityId) {
        int accent = qualityId.map(RarityPalette::qualityColor).orElseGet(() -> color(rarity));
        int inner = darken(accent, 0.45f);
        int bgTop = (darken(accent, 0.82f) & 0x00FFFFFF) | 0xF0000000;
        int bgBottom = (darken(accent, 0.90f) & 0x00FFFFFF) | 0xF0000000;
        int titleBar = (darken(accent, 0.35f) & 0x00FFFFFF) | 0x80000000;
        int slot = (darken(accent, 0.55f) & 0x00FFFFFF) | 0xE0000000;
        return new TooltipTheme(
                accent,
                inner,
                bgTop,
                bgBottom,
                0xFFFFFFFF,
                0xFF2A2A30,
                0xFFECECEC,
                0xFFFFD5A0,
                0xFFE6ECF5,
                inner,
                titleBar,
                slot,
                brighten(accent, 0.25f)
        );
    }

    public static int darken(int argb, float amount) {
        return scale(argb, 1.0f - clamp(amount));
    }

    public static int brighten(int argb, float amount) {
        int a = (argb >>> 24) & 0xFF;
        float t = clamp(amount);
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        r = (int) (r + (255 - r) * t);
        g = (int) (g + (255 - g) * t);
        b = (int) (b + (255 - b) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int scale(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, (int) (((argb >>> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((argb >>> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((argb & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
