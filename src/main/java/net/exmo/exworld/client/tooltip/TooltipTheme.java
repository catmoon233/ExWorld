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
) {}
