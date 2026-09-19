package net.exmo.exworld.ship.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** One non-empty mounted inventory slot. Extra is an opaque SNBT blob and may be empty. */
public record ShipSlot(int index, String itemId, int count, String extra) {
    public ShipSlot {
        if (index < 0 || index > 255) throw new IllegalArgumentException("slot index out of range");
        if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("slot item is blank");
        if (count < 1 || count > 99) throw new IllegalArgumentException("slot count out of range");
        extra = extra == null ? "" : extra;
    }

    public static ShipSlot of(int index, String itemId, int count) {
        return new ShipSlot(index, itemId, count, "");
    }

    public static List<ShipSlot> copy(List<ShipSlot> slots) {
        if (slots == null || slots.isEmpty()) return List.of();
        return Collections.unmodifiableList(new ArrayList<>(slots));
    }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ShipSlot slot)) return false;
        return index == slot.index && count == slot.count && itemId.equals(slot.itemId) && extra.equals(slot.extra);
    }

    @Override public int hashCode() { return Objects.hash(index, itemId, count, extra); }
}
