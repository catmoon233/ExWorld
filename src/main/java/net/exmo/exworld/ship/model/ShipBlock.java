package net.exmo.exworld.ship.model;

import java.util.List;

/** One local hull voxel. Coordinates are relative to the hull origin. */
public record ShipBlock(int x, int y, int z, String block, List<ShipSlot> slots) {
    public ShipBlock {
        block = block == null ? "" : block;
        slots = ShipSlot.copy(slots);
    }

    public int packed() { return ShipOccupancy.pack(x, y, z); }
    public boolean hasContainer() { return !slots.isEmpty(); }
}
