package net.exmo.exworld.client.tooltip;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

import java.util.Optional;

/**
 * ColorTooltips-style rarity colours. Common uses a silver frame on a dark panel so
 * white-on-white never happens; higher rarities keep saturated accents on darkened fills.
 */
public final class RarityPalette {
    public static final int COMMON = 0xFF9AA3B0;
    public static final int UNCOMMON = 0xFFE8D24A;
    public static final int RARE = 0xFF3AD4E8;
    public static final int EPIC = 0xFFE060F0;
    public static final int LEGENDARY = 0xFFFFAA00;

    private static final int COMMON_BG_TOP = 0xF0121218;
    private static final int COMMON_BG_BOTTOM = 0xF008080C;
    private static final int COMMON_TITLE = 0xE0181820;
    private static final int COMMON_SLOT = 0xE024242C;

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
        return themeOf(accent);
    }

    public static TooltipTheme themeOf(int accent) {
        boolean common = isCommon(accent);
        int inner = common ? 0xFF3A3E46 : darken(accent, 0.50f);
        int bgTop = common ? COMMON_BG_TOP : (darken(accent, 0.88f) & 0x00FFFFFF) | 0xF0000000;
        int bgBottom = common ? COMMON_BG_BOTTOM : (darken(accent, 0.93f) & 0x00FFFFFF) | 0xF0000000;
        int titleBar = common ? COMMON_TITLE : (darken(accent, 0.55f) & 0x00FFFFFF) | 0xE0000000;
        int slot = common ? COMMON_SLOT : (darken(accent, 0.62f) & 0x00FFFFFF) | 0xE0000000;
        int flow = common ? brighten(accent, 0.35f) : brighten(accent, 0.28f);
        return new TooltipTheme(
                accent,
                inner,
                bgTop,
                bgBottom,
                0xFFF4F4F7,
                0xFF1E1E26,
                0xFFF0F0F4,
                0xFFFFD5A0,
                0xFFE6ECF5,
                inner,
                titleBar,
                slot,
                flow
        );
    }

    public static int contrastText(int background) {
        return luminance(background) > 0.55f ? 0xFF16161C : 0xFFF4F4F7;
    }

    public static int lerp(int a, int b, float t) {
        t = clamp(t);
        int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int ra = (int) (aa + (ba - aa) * t);
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
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

    public static boolean isCommon(int accent) {
        int r = (accent >>> 16) & 0xFF;
        int g = (accent >>> 8) & 0xFF;
        int b = accent & 0xFF;
        int span = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
        return span < 40 && luminance(accent) > 0.45f;
    }

    public static float luminance(int argb) {
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        return 0.2126f * r + 0.7152f * g + 0.0722f * b;
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
