package net.exmo.exworld.client.tooltip;

import java.util.List;

public final class TooltipLayoutTestHarness {
    public static void main(String[] args) {
        List<Chip> chips = List.of(
                new Chip("Sharp Lv.2", 0xFFDB5E71),
                new Chip("Fire 3", 0xFFFF8A4A),
                new Chip("Guard Lv.1", 0xFF5E8ACF)
        );
        List<List<Chip>> narrow = TooltipLayout.wrapChips(chips, String::length, 12);
        if (narrow.size() != 3) {
            throw new AssertionError("narrow width should wrap to one chip per row, got " + narrow.size());
        }
        for (List<Chip> row : narrow) {
            if (row.size() != 1) throw new AssertionError("narrow row should hold a single chip");
        }
        List<List<Chip>> wide = TooltipLayout.wrapChips(chips, String::length, 80);
        if (wide.size() != 1 || wide.getFirst().size() != 3) {
            throw new AssertionError("wide width should keep chips on one row");
        }

        int tags = NameTag.rowWidth(
                List.of(new NameTag("SWORD", 1), new NameTag("Common", 2)),
                String::length,
                TooltipLayout.CHIP_PAD_H,
                TooltipLayout.TAG_GAP
        );
        int header = TooltipLayout.headerContentWidth("Iron Sword".length(), tags, "Common".length());
        int titleOnly = TooltipLayout.headerContentWidth("Iron Sword".length(), 0, "Common".length());
        if (header <= titleOnly) {
            throw new AssertionError("name tags must increase header width");
        }
        if (header < TooltipLayout.TITLE_OFFSET + "Iron Sword".length() + tags) {
            throw new AssertionError("header width must include title offset and tags");
        }
        System.out.println("Tooltip layout tests passed");
    }
}
