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

        List<NameTag> nameTags = List.of(
                new NameTag("SWORD", 1),
                new NameTag("Common", 2),
                new NameTag("LongOverflowTag", 3)
        );
        java.util.function.IntUnaryOperator tagW = TooltipLayout::chipWidth;
        List<List<NameTag>> wrappedNarrow = NameTag.wrap(
                nameTags,
                tag -> tagW.applyAsInt(tag.label().length()),
                10,
                10,
                TooltipLayout.TAG_GAP
        );
        if (wrappedNarrow.size() != 3) {
            throw new AssertionError("name tags should wrap when the title row is full, got " + wrappedNarrow.size());
        }
        List<List<NameTag>> wrappedWide = NameTag.wrap(
                nameTags,
                tag -> tagW.applyAsInt(tag.label().length()),
                60,
                60,
                TooltipLayout.TAG_GAP
        );
        if (wrappedWide.size() != 1 || wrappedWide.getFirst().size() != 3) {
            throw new AssertionError("name tags should stay on one row when there is room");
        }
        if (TooltipLayout.headerHeight() != 27) {
            throw new AssertionError("header must stay compact enough for title and rarity rows");
        }
        System.out.println("Tooltip layout tests passed");
    }
}
