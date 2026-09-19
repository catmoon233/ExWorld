package net.exmo.exworld.content.item;

import net.minecraft.world.item.Item;

/** Equipped storage core that unlocks backpack extension cells. */
public final class StorageCoreItem extends Item {
    private final int extensionCells;

    public StorageCoreItem(Properties properties, int extensionCells) {
        super(properties);
        this.extensionCells = Math.max(0, extensionCells);
    }

    public int extensionCells() {
        return extensionCells;
    }
}
