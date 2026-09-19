package net.exmo.exworld.client.tooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Left-to-right chip packing that wraps when the next chip would exceed {@code maxWidth}. */
public final class ChipRow {
    private ChipRow() {}

    public static List<List<Chip>> wrap(List<Chip> chips, ToIntFunction<Chip> widthOf, int maxWidth, int gap) {
        if (chips == null || chips.isEmpty()) return List.of();
        int cap = Math.max(1, maxWidth);
        List<List<Chip>> rows = new ArrayList<>();
        List<Chip> current = new ArrayList<>();
        int rowWidth = 0;
        for (Chip chip : chips) {
            int width = Math.max(0, widthOf.applyAsInt(chip));
            int extra = current.isEmpty() ? width : rowWidth + gap + width;
            if (!current.isEmpty() && extra > cap) {
                rows.add(List.copyOf(current));
                current = new ArrayList<>();
                current.add(chip);
                rowWidth = width;
            } else {
                current.add(chip);
                rowWidth = extra;
            }
        }
        if (!current.isEmpty()) rows.add(List.copyOf(current));
        return List.copyOf(rows);
    }
}
