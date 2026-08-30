package net.exmo.exworld.world.model;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public record TravelAnchor(String id, String name, BlockPos pos, String tileId) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("name", name);
        tag.putLong("pos", pos.asLong());
        tag.putString("tile_id", tileId);
        return tag;
    }

    public static TravelAnchor load(CompoundTag tag) {
        return new TravelAnchor(tag.getString("id"), tag.getString("name"), BlockPos.of(tag.getLong("pos")), tag.getString("tile_id"));
    }
}
