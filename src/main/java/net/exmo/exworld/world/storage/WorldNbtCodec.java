package net.exmo.exworld.world.storage;

import net.exmo.exworld.world.model.Region;
import net.exmo.exworld.world.model.WorldBiome;
import net.exmo.exworld.world.model.WorldTile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.List;

/** Persistence adapter kept out of the world domain model. */
final class WorldNbtCodec {
    private WorldNbtCodec() {}

    static CompoundTag saveTile(WorldTile tile) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", tile.id()); tag.putInt("map_x", tile.mapX()); tag.putInt("map_z", tile.mapZ());
        tag.putString("region_id", tile.regionId()); tag.putString("name", tile.name()); tag.putInt("color", tile.color());
        tag.putInt("world_x", tile.worldX()); tag.putInt("world_z", tile.worldZ()); tag.putBoolean("discovered", tile.discovered());
        tag.putString("biome_id", tile.biomeId()); tag.putString("description", tile.description());
        tag.putString("sites", tile.sites()); tag.putString("resources", tile.resources());
        return tag;
    }

    static WorldTile loadTile(CompoundTag tag) {
        return new WorldTile(tag.getString("id"), tag.getInt("map_x"), tag.getInt("map_z"), tag.getString("region_id"),
                tag.getString("name"), tag.getInt("color"), tag.getInt("world_x"), tag.getInt("world_z"),
                tag.getBoolean("discovered"), tag.contains("biome_id") ? tag.getString("biome_id") : WorldBiome.PRAIRIE.id(),
                tag.getString("description"), tag.contains("sites") ? tag.getString("sites") : tag.getString("buildings"),
                tag.getString("resources"));
    }

    static CompoundTag saveRegion(Region region) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", region.id());
        ListTag tiles = new ListTag();
        region.tileIds().forEach(tileId -> tiles.add(StringTag.valueOf(tileId)));
        tag.put("tile_ids", tiles); tag.putString("name", region.name()); tag.putLong("story_seed", region.storySeed());
        tag.putString("icon", region.icon());
        tag.putString("site", region.site()); tag.putString("resources", region.resources());
        tag.putBoolean("configured", region.configured());
        return tag;
    }

    static Region loadRegion(CompoundTag tag) {
        ListTag tileTags = tag.getList("tile_ids", StringTag.TAG_STRING);
        List<String> tileIds = tileTags.isEmpty() && tag.contains("tile_id") ? List.of(tag.getString("tile_id"))
                : tileTags.stream().map(value -> value.getAsString()).toList();
        return new Region(tag.getString("id"), tileIds, tag.getString("name"), tag.getLong("story_seed"),
                tag.contains("icon") ? tag.getString("icon") : "", tag.contains("site") ? tag.getString("site") : "",
                tag.contains("resources") ? tag.getString("resources") : "", tag.getBoolean("configured"));
    }
}
