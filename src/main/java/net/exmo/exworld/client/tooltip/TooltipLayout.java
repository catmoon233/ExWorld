package net.exmo.exworld.client.tooltip;

import java.util.List;
import java.util.function.ToIntFunction;

/** Shared geometry for the themed tooltip. Numbers follow EnhancedTooltips header spacing. */
public final class TooltipLayout {
    public static final int PAD = 10;
    public static final int TITLE_OFFSET = 30;
    public static final int SLOT = 20;
    public static final int ICON = 16;
    public static final int LINE = 9;
    public static final int CHIP_PAD_H = 3;
    public static final int CHIP_GAP = 3;
    public static final int TAG_GAP = 3;
    public static final int ROW_GAP = 3;
    public static final int MAX_TEXT = 240;
    public static final int MAX_BODY = 220;
    public static final int MIN_WIDTH = 120;

    private TooltipLayout() {}

    public static int headerHeight() {
        return 27;
    }

    public static int headerContentWidth(int titleWidth, int nameTagsWidth, int rarityWidth) {
        int titleRow = Math.max(0, titleWidth) + (nameTagsWidth > 0 ? TAG_GAP + nameTagsWidth : 0);
        return TITLE_OFFSET + Math.max(titleRow, Math.max(0, rarityWidth));
    }

    public static int chipWidth(int textWidth) {
        return Math.max(0, textWidth) + CHIP_PAD_H * 2;
    }

    public static List<List<Chip>> wrapChips(List<Chip> chips, ToIntFunction<String> textWidth, int maxWidth) {
        return ChipRow.wrap(chips, chip -> chipWidth(textWidth.applyAsInt(chip.label())), maxWidth, CHIP_GAP);
    }

    public static int chipBlockHeight(int rows) {
        if (rows <= 0) return 0;
        return rows * LINE + (rows - 1) * ROW_GAP;
    }

    public static int tagBlockHeight(int rows) {
        if (rows <= 0) return 0;
        return rows * LINE + (rows - 1) * ROW_GAP;
    }
}
