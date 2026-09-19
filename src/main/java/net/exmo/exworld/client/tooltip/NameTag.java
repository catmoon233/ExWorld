package net.exmo.exworld.client.tooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Small coloured tag drawn on the title row, EnhancedTooltips-style. */
public record NameTag(String label, int color) {
    public NameTag {
        label = label == null ? "" : label;
    }

    public static int rowWidth(List<NameTag> tags, ToIntFunction<String> textWidth, int padH, int gap) {
        if (tags == null || tags.isEmpty()) return 0;
        int width = 0;
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) width += gap;
            width += textWidth.applyAsInt(tags.get(i).label()) + padH * 2;
        }
        return width;
    }

    public static List<List<NameTag>> wrap(
            List<NameTag> tags,
            ToIntFunction<NameTag> widthOf,
            int firstRowWidth,
            int laterRowWidth,
            int gap
    ) {
        if (tags == null || tags.isEmpty()) return List.of();
        int firstCap = Math.max(1, firstRowWidth);
        int laterCap = Math.max(1, laterRowWidth);
        List<List<NameTag>> rows = new ArrayList<>();
        List<NameTag> current = new ArrayList<>();
        int rowWidth = 0;
        int cap = firstCap;
        for (NameTag tag : tags) {
            int width = Math.max(0, widthOf.applyAsInt(tag));
            if (current.isEmpty() && width > cap && rows.isEmpty()) {
                cap = laterCap;
            }
            int extra = current.isEmpty() ? width : rowWidth + gap + width;
            if (!current.isEmpty() && extra > cap) {
                rows.add(List.copyOf(current));
                current = new ArrayList<>();
                current.add(tag);
                rowWidth = width;
                cap = laterCap;
            } else {
                current.add(tag);
                rowWidth = extra;
            }
        }
        if (!current.isEmpty()) rows.add(List.copyOf(current));
        return List.copyOf(rows);
    }
}
