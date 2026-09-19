package net.exmo.exworld.client.tooltip;

/** Panel colours resolved from rarity, with optional quality override on the frame. */
public record TooltipTheme(
        int border,
        int borderInner,
        int bgTop,
        int bgBottom,
        int name,
        int badgeBg,
        int badgeCutout,
        int sectionHeader,
        int body,
        int separator,
        int titleBar,
        int slotFill,
        int flow
) {
    public TooltipTheme withAlpha(float alpha) {
        float a = Math.max(0f, Math.min(1f, alpha));
        return new TooltipTheme(
                alpha(border, a),
                alpha(borderInner, a),
                alpha(bgTop, a),
                alpha(bgBottom, a),
                alpha(name, a),
                alpha(badgeBg, a),
                alpha(badgeCutout, a),
                alpha(sectionHeader, a),
                alpha(body, a),
                alpha(separator, a),
                alpha(titleBar, a),
                alpha(slotFill, a),
                alpha(flow, a)
        );
    }

    private static int alpha(int color, float a) {
        int alpha = (int) ((color >>> 24) * a);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
