package net.exmo.exworld.client.battle;

import net.exmo.exworld.battle.api.BattleSnapshot;

import java.util.List;

/** Regression checks for the combat HUD's selectable inventory and card render budget. */
public final class BattleHudTestHarness {
    public static void main(String[] args) {
        hidesItemsThatCannotBeUsed();
        scrollsAllVisibleConsumablesWithoutOvershooting();
        powerCardsUseOneDrawCallPerScanline();
        mergedLogLinesNeverUseTheOriginalEventCount();
    }

    private static void hidesItemsThatCannotBeUsed() {
        List<BattleSnapshot.ItemView> items = List.of(
                item(0, "minecraft:potion", true),
                item(1, "minecraft:stick", false),
                item(2, "exworld:battle_elixir", true),
                item(3, "minecraft:apple", false));

        List<BattleSnapshot.ItemView> visible = BattleHud.usableItems(items);

        check(visible.equals(List.of(items.getFirst(), items.get(2))),
                "the battle backpack only exposes consumables that can currently be used");
    }

    private static void scrollsAllVisibleConsumablesWithoutOvershooting() {
        check(BattleHud.maxItemScroll(14, 6, 2) == 4,
                "four two-column scroll steps expose fourteen consumables through six slots");
        check(BattleHud.scrollItemRows(0, -1, 14, 6, 2) == 1,
                "scrolling down advances the backpack by one row");
        check(BattleHud.scrollItemRows(4, -1, 14, 6, 2) == 4,
                "the backpack cannot scroll beyond its final row");
        check(BattleHud.scrollItemRows(0, 1, 14, 6, 2) == 0,
                "the backpack cannot scroll above its first row");
    }

    private static void powerCardsUseOneDrawCallPerScanline() {
        int width = 84, height = 128;
        check(BattleHud.powerCardFrameDrawCalls(width, height) <= Math.min(width, height) + 5,
                "a power card frame must use scanline-sized draw work, never per-pixel fills");
    }

    private static void mergedLogLinesNeverUseTheOriginalEventCount() {
        check(BattleHud.visibleLogIndexes(9, 3, 0).equals(List.of(6, 7, 8)),
                "a merged nine-line log renders only indexes that exist when the viewport has three rows");
        check(BattleHud.visibleLogIndexes(9, 3, 2).equals(List.of(4, 5, 6)),
                "scrolling a merged log also stays within its compacted line list");
    }

    private static BattleSnapshot.ItemView item(int inventorySlot, String itemId, boolean usable) {
        return new BattleSnapshot.ItemView(inventorySlot, itemId, itemId, 1, usable, "self");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
