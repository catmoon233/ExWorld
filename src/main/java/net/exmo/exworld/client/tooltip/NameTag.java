package net.exmo.exworld.client.tooltip;

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
}
