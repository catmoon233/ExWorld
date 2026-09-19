package net.exmo.exworld.inventory;

import java.util.LinkedHashMap;
import java.util.Map;

/** Item-id → footprint table. Defaults cover a few tools; creative edit overwrites entries. */
public final class FootprintRules {
    private final Map<String, ItemFootprint> exact = new LinkedHashMap<>();

    public static FootprintRules defaults() {
        FootprintRules rules = new FootprintRules();
        rules.set("minecraft:wooden_sword", ItemFootprint.of(1, 3));
        rules.set("minecraft:stone_sword", ItemFootprint.of(1, 3));
        rules.set("minecraft:iron_sword", ItemFootprint.of(1, 3));
        rules.set("minecraft:golden_sword", ItemFootprint.of(1, 3));
        rules.set("minecraft:diamond_sword", ItemFootprint.of(1, 3));
        rules.set("minecraft:netherite_sword", ItemFootprint.of(1, 3));
        rules.set("minecraft:bow", ItemFootprint.of(2, 3));
        rules.set("minecraft:crossbow", ItemFootprint.of(2, 3));
        rules.set("minecraft:chest", ItemFootprint.of(2, 2));
        rules.set("minecraft:ender_chest", ItemFootprint.of(2, 2));
        rules.set("exworld:warrior_blade", ItemFootprint.of(1, 3));
        return rules;
    }

    public void set(String itemId, ItemFootprint footprint) {
        if (itemId == null || itemId.isBlank() || footprint == null) return;
        exact.put(itemId, footprint);
    }

    public void clear(String itemId) {
        if (itemId != null) exact.remove(itemId);
    }

    public ItemFootprint of(String itemId) {
        if (itemId == null || itemId.isBlank()) return ItemFootprint.UNIT;
        return exact.getOrDefault(itemId, ItemFootprint.UNIT);
    }

    public Map<String, ItemFootprint> snapshot() {
        return Map.copyOf(exact);
    }

    public void replaceAll(Map<String, ItemFootprint> values) {
        exact.clear();
        if (values == null) return;
        values.forEach(this::set);
    }
}
