package net.exmo.exworld.world.storage;

import net.exmo.exworld.Config;
import net.exmo.exworld.world.generation.WorldLayoutGenerator;
import net.exmo.exworld.world.model.Region;
import net.exmo.exworld.world.model.WorldTile;
import net.exmo.exworld.world.model.WorldDimensions;
import net.exmo.exworld.world.model.TravelAnchor;
import net.exmo.exworld.world.model.ManualChunkGroupLayout;
import net.exmo.exworld.world.model.MapRegion;
import net.exmo.exworld.world.model.WorldBiome;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

public final class WorldStateData extends SavedData {
    private static final int LAYOUT_VERSION = 8;
    public static final Factory<WorldStateData> FACTORY = new Factory<>(WorldStateData::new, WorldStateData::load);
    private final Map<String, WorldTile> tiles = new LinkedHashMap<>();
    private final Map<String, Region> regions = new LinkedHashMap<>();
    private final Map<UUID, String> playerTiles = new LinkedHashMap<>();
    private final Map<String, TravelAnchor> anchors = new LinkedHashMap<>();
    private final Map<UUID, List<String>> playerAnchors = new LinkedHashMap<>();
    private final Map<UUID, Long> anchorCooldowns = new LinkedHashMap<>();
    private final Map<UUID, String> pendingRespawns = new LinkedHashMap<>();
    private int totalChunks;
    private int groupChunks = WorldDimensions.DEFAULT_GROUP_CHUNKS;
    private final BitSet generatedChunkBits = new BitSet();
    private boolean pregenerationEnabled;
    /** New worlds begin with biome-generated zones; the editor can still switch to manual singleton groups. */
    private boolean manualGroups = false;
    /** Monotonic edit token so concurrent remote editors cannot silently overwrite one another. */
    private long groupRevision;
    private long worldSeed;
    private boolean archipelagoSpawnApplied;
    private int layoutVersion = LAYOUT_VERSION;

    public void initialize(long seed) {
        worldSeed = seed;
        if (layoutVersion != LAYOUT_VERSION) {
            tiles.clear();
            regions.clear();
            generatedChunkBits.clear();
        }
        if (!tiles.isEmpty()) return;
        WorldLayoutGenerator.GeneratedLayout layout = WorldLayoutGenerator.generate(seed, groupChunks, !manualGroups);
        layout.tiles().forEach(tile -> tiles.put(tile.id(), tile));
        layout.regions().forEach(region -> regions.put(region.id(), region));
        totalChunks = tiles.size() * WorldDimensions.chunksPerGroup(groupChunks);
        layoutVersion = LAYOUT_VERSION;
        setDirty();
    }

    public List<WorldTile> tiles() { return List.copyOf(tiles.values()); }
    public Optional<Region> region(String id) { return Optional.ofNullable(regions.get(id)); }
    public List<MapRegion> regionInfos(Set<String> regionIds) { return regions.values().stream()
            .filter(region -> regionIds.contains(region.id()))
            .map(region -> new MapRegion(region.id(), region.name(), region.icon(), region.site(), region.resources(),
                    region.configured())).toList(); }
    public Optional<WorldTile> tile(String id) { return Optional.ofNullable(tiles.get(id)); }
    /** Materializes one previously unseen world cell as an unconfigured singleton group. */
    public Optional<WorldTile> ensureTile(int mapX, int mapZ) {
        if (!WorldDimensions.withinMaximumWorldBorder(mapX, mapZ, groupChunks)) return Optional.empty();
        if (!Config.autoExpandTiles && !tiles.containsKey(tileId(mapX, mapZ))) return Optional.empty();
        String id = tileId(mapX, mapZ);
        WorldTile existing = tiles.get(id);
        if (existing != null) return Optional.of(existing);
        WorldBiome biome = WorldBiome.values()[Math.floorMod(expansionHash(worldSeed, mapX, mapZ), WorldBiome.values().length)];
        String regionId = (manualGroups ? "manual_" : "automatic_") + id;
        String name = "未设置区域 [" + mapX + ", " + mapZ + "]";
        WorldTile created = new WorldTile(id, mapX, mapZ, regionId, name, biome.mapColor(),
                WorldDimensions.groupCenter(mapX, groupChunks), WorldDimensions.groupCenter(mapZ, groupChunks), false,
                biome.id(), biome.displayName() + "延伸至此。", "", biome.resources());
        tiles.put(id, created);
        regions.put(regionId, new Region(regionId, List.of(id), name, worldSeed ^ id.hashCode(), "", "", "", false));
        totalChunks += WorldDimensions.chunksPerGroup(groupChunks);
        // The pre-generation queue is deliberately bounded to the original window. New cells are generated on demand.
        pregenerationEnabled = false;
        groupRevision++;
        setDirty();
        return Optional.of(created);
    }
    public int generatedChunks() { return generatedChunkBits.cardinality(); }
    public int totalChunks() { return totalChunks; }
    public int groupChunks() { return groupChunks; }
    public boolean pregenerationEnabled() { return pregenerationEnabled; }
    public boolean manualGroups() { return manualGroups; }
    public long groupRevision() { return groupRevision; }
    public boolean archipelagoSpawnApplied() { return archipelagoSpawnApplied; }
    public void markArchipelagoSpawnApplied() {
        if (archipelagoSpawnApplied) return;
        archipelagoSpawnApplied = true;
        setDirty();
    }
    public List<TravelAnchor> anchors() { return List.copyOf(anchors.values()); }
    public Optional<TravelAnchor> anchor(String id) { return Optional.ofNullable(anchors.get(id)); }
    public List<String> playerAnchors(UUID playerId) { return List.copyOf(playerAnchors.getOrDefault(playerId, List.of())); }
    public long anchorCooldown(UUID playerId) { return anchorCooldowns.getOrDefault(playerId, 0L); }
    public Optional<String> pendingRespawn(UUID playerId) { return Optional.ofNullable(pendingRespawns.get(playerId)); }

    /** Rebuilds physical coordinates while retaining both manual and legacy group membership. */
    public void resizeGroups(long seed, int chunks) {
        groupChunks = WorldDimensions.validateGroupChunks(chunks);
        List<TravelAnchor> existingAnchors = List.copyOf(anchors.values());
        tiles.replaceAll((id, tile) -> new WorldTile(tile.id(), tile.mapX(), tile.mapZ(), tile.regionId(), tile.name(),
                tile.color(), WorldDimensions.groupCenter(tile.mapX(), groupChunks),
                WorldDimensions.groupCenter(tile.mapZ(), groupChunks), tile.discovered(), tile.biomeId(),
                tile.description(), tile.sites(), tile.resources()));
        playerTiles.clear();
        generatedChunkBits.clear();
        totalChunks = tiles.size() * WorldDimensions.chunksPerGroup(groupChunks);
        anchors.clear();
        for (TravelAnchor anchor : existingAnchors) {
            int mapX = clampMapCoordinate(WorldDimensions.groupCoordinate(anchor.pos().getX(), groupChunks));
            int mapZ = clampMapCoordinate(WorldDimensions.groupCoordinate(anchor.pos().getZ(), groupChunks));
            String tileId = tileId(mapX, mapZ);
            anchors.put(anchor.id(), new TravelAnchor(anchor.id(), anchor.name(), anchor.pos(), tileId));
        }
        groupRevision++;
        setDirty();
    }

    /** Enabling preserves existing generated groups; disabling deliberately restores the generated partition. */
    public void setManualGroups(long seed, boolean enabled) {
        if (manualGroups == enabled) return;
        if (!enabled) replaceLayout(WorldLayoutGenerator.generate(seed, groupChunks, true));
        manualGroups = enabled;
        groupRevision++;
        setDirty();
    }

    /** Applies a complete editor draft after its coverage and connectedness have been verified in one place. */
    public void applyGroupEdit(long seed, boolean enabled, List<ManualChunkGroupLayout.Group> groups) {
        if (!enabled) {
            setManualGroups(seed, false);
            return;
        }
        Set<String> editedTileIds = new HashSet<>();
        groups.forEach(group -> editedTileIds.addAll(group.tileIds()));
        List<WorldTile> editedTiles = tiles.values().stream().filter(tile -> editedTileIds.contains(tile.id())).toList();
        if (editedTiles.size() != editedTileIds.size()) throw new IllegalArgumentException("editor draft includes an unknown world tile");
        ManualChunkGroupLayout.Applied applied = ManualChunkGroupLayout.apply(editedTiles, groups);
        if (editedTiles.size() == tiles.size()) {
            tiles.clear();
            applied.tiles().forEach(tile -> tiles.put(tile.id(), tile));
            regions.clear();
            applied.regions().forEach(region -> regions.put(region.id(), region));
        } else {
            applyWindowedGroupEdit(editedTileIds, applied);
        }
        manualGroups = true;
        groupRevision++;
        setDirty();
    }

    /** Replaces only the loaded editor window while detaching any old region members that lie beyond it. */
    private void applyWindowedGroupEdit(Set<String> editedTileIds, ManualChunkGroupLayout.Applied applied) {
        Set<String> editedRegionIds = tiles.values().stream().filter(tile -> editedTileIds.contains(tile.id()))
                .map(WorldTile::regionId).collect(java.util.stream.Collectors.toSet());
        Set<String> replacementIds = applied.regions().stream().map(Region::id).collect(java.util.stream.Collectors.toSet());
        for (String replacementId : replacementIds) {
            if (regions.containsKey(replacementId) && !editedRegionIds.contains(replacementId)) {
                throw new IllegalArgumentException("group id is already used outside this map window: " + replacementId);
            }
        }
        for (String regionId : editedRegionIds) {
            Region old = regions.remove(regionId);
            List<WorldTile> outside = tiles.values().stream().filter(tile -> tile.regionId().equals(regionId)
                    && !editedTileIds.contains(tile.id())).toList();
            if (outside.isEmpty()) continue;
            String detachedId = detachedRegionId(regionId, replacementIds);
            Region source = old == null ? new Region(regionId, List.of(), regionId, 0L) : old;
            regions.put(detachedId, new Region(detachedId, outside.stream().map(WorldTile::id).toList(), source.name(),
                    source.storySeed(), source.icon(), source.site(), source.resources(), source.configured()));
            for (WorldTile tile : outside) tiles.put(tile.id(), withRegion(tile, detachedId));
        }
        applied.tiles().forEach(tile -> tiles.put(tile.id(), tile));
        applied.regions().forEach(region -> regions.put(region.id(), region));
    }

    private String detachedRegionId(String originalId, Set<String> reservedIds) {
        for (int suffix = 1; ; suffix++) {
            String id = originalId + "_outside_" + suffix;
            if (!regions.containsKey(id) && !reservedIds.contains(id)) return id;
        }
    }

    private static WorldTile withRegion(WorldTile tile, String regionId) {
        return new WorldTile(tile.id(), tile.mapX(), tile.mapZ(), regionId, tile.name(), tile.color(), tile.worldX(),
                tile.worldZ(), tile.discovered(), tile.biomeId(), tile.description(), tile.sites(), tile.resources());
    }

    private void replaceLayout(WorldLayoutGenerator.GeneratedLayout layout) {
        tiles.clear();
        regions.clear();
        layout.tiles().forEach(tile -> tiles.put(tile.id(), tile));
        layout.regions().forEach(region -> regions.put(region.id(), region));
    }

    private static String tileId(int x, int z) {
        return "tile_" + (x < 0 ? "n" + -x : "p" + x) + "_" + (z < 0 ? "n" + -z : "p" + z);
    }

    private int clampMapCoordinate(int coordinate) {
        int maximum = WorldDimensions.maximumGroupCoordinate(groupChunks);
        return Math.max(-maximum, Math.min(maximum, coordinate));
    }

    private static int expansionHash(long seed, int x, int z) {
        long mixed = seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        return (int) (mixed ^ mixed >>> 31);
    }

    public void registerAnchor(TravelAnchor anchor) {
        anchors.put(anchor.id(), anchor);
        setDirty();
    }

    public boolean bindAnchor(UUID playerId, String anchorId) {
        List<String> bound = playerAnchors.computeIfAbsent(playerId, ignored -> new ArrayList<>());
        if (bound.contains(anchorId)) return false;
        bound.add(anchorId);
        setDirty();
        return true;
    }

    public void setAnchorCooldown(UUID playerId, long readyAt) {
        anchorCooldowns.put(playerId, readyAt);
        setDirty();
    }

    public void setPendingRespawn(UUID playerId, String anchorId) {
        if (anchorId.isEmpty()) pendingRespawns.remove(playerId); else pendingRespawns.put(playerId, anchorId);
        setDirty();
    }
    public String playerTile(UUID playerId) { return playerTiles.getOrDefault(playerId, "tile_p0_p0"); }

    public void setPlayerTile(UUID playerId, String tileId) {
        if (tileId.equals(playerTiles.get(playerId))) return;
        playerTiles.put(playerId, tileId);
        setDirty();
    }

    public void setPregenerationEnabled(boolean enabled) {
        if (pregenerationEnabled == enabled) return;
        pregenerationEnabled = enabled;
        setDirty();
    }

    /** Records arbitrary vanilla chunk generation, whether caused by a player or the optional background queue. */
    public void markChunkGenerated(int chunkX, int chunkZ) {
        int index = WorldDimensions.generationIndex(chunkX, chunkZ, groupChunks);
        if (index < 0 || generatedChunkBits.get(index)) return;
        generatedChunkBits.set(index);
        setDirty();
    }

    public int nextMissingChunkIndex() {
        int index = generatedChunkBits.nextClearBit(0);
        return index < totalChunks ? index : -1;
    }

    public BitSet completedTileIndices() {
        BitSet completed = new BitSet(tiles.size());
        int chunksPerTile = WorldDimensions.chunksPerGroup(groupChunks);
        for (int tileIndex = 0; tileIndex < tiles.size(); tileIndex++) {
            int start = tileIndex * chunksPerTile;
            if (generatedChunkBits.nextClearBit(start) >= start + chunksPerTile) completed.set(tileIndex);
        }
        return completed;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag tileTags = new ListTag();
        tiles.values().forEach(tile -> tileTags.add(WorldNbtCodec.saveTile(tile)));
        tag.put("tiles", tileTags);
        ListTag regionTags = new ListTag();
        regions.values().forEach(region -> regionTags.add(WorldNbtCodec.saveRegion(region)));
        tag.put("regions", regionTags);
        ListTag playerTags = new ListTag();
        playerTiles.forEach((playerId, tileId) -> {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("player_id", playerId);
            playerTag.putString("tile_id", tileId);
            playerTags.add(playerTag);
        });
        tag.put("player_tiles", playerTags);
        ListTag anchorTags = new ListTag();
        anchors.values().forEach(anchor -> anchorTags.add(anchor.save()));
        tag.put("anchors", anchorTags);
        ListTag bindingTags = new ListTag();
        playerAnchors.forEach((playerId, anchorIds) -> {
            for (String anchorId : anchorIds) {
                CompoundTag binding = new CompoundTag();
                binding.putUUID("player_id", playerId);
                binding.putString("anchor_id", anchorId);
                bindingTags.add(binding);
            }
        });
        tag.put("anchor_bindings", bindingTags);
        ListTag cooldownTags = new ListTag();
        anchorCooldowns.forEach((playerId, readyAt) -> {
            CompoundTag cooldown = new CompoundTag();
            cooldown.putUUID("player_id", playerId);
            cooldown.putLong("ready_at", readyAt);
            cooldownTags.add(cooldown);
        });
        tag.put("anchor_cooldowns", cooldownTags);
        ListTag respawnTags = new ListTag();
        pendingRespawns.forEach((playerId, anchorId) -> {
            CompoundTag respawn = new CompoundTag();
            respawn.putUUID("player_id", playerId);
            respawn.putString("anchor_id", anchorId);
            respawnTags.add(respawn);
        });
        tag.put("pending_respawns", respawnTags);
        tag.putInt("layout_version", layoutVersion);
        tag.putLongArray("generated_chunk_bits", generatedChunkBits.toLongArray());
        tag.putInt("total_chunks", totalChunks);
        tag.putInt("group_chunks", groupChunks);
        tag.putBoolean("pregeneration_enabled", pregenerationEnabled);
        tag.putBoolean("manual_groups", manualGroups);
        tag.putLong("group_revision", groupRevision);
        tag.putLong("world_seed", worldSeed);
        tag.putBoolean("archipelago_spawn_applied", archipelagoSpawnApplied);
        return tag;
    }

    static WorldStateData load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldStateData data = new WorldStateData();
        data.layoutVersion = tag.getInt("layout_version");
        ListTag tileTags = tag.getList("tiles", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < tileTags.size(); i++) {
            WorldTile tile = WorldNbtCodec.loadTile(tileTags.getCompound(i));
            data.tiles.put(tile.id(), tile);
        }
        ListTag regionTags = tag.getList("regions", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < regionTags.size(); i++) {
            Region region = WorldNbtCodec.loadRegion(regionTags.getCompound(i));
            data.regions.put(region.id(), region);
        }
        ListTag playerTags = tag.getList("player_tiles", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < playerTags.size(); i++) {
            CompoundTag playerTag = playerTags.getCompound(i);
            data.playerTiles.put(playerTag.getUUID("player_id"), playerTag.getString("tile_id"));
        }
        ListTag anchorTags = tag.getList("anchors", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < anchorTags.size(); i++) {
            TravelAnchor anchor = TravelAnchor.load(anchorTags.getCompound(i));
            data.anchors.put(anchor.id(), anchor);
        }
        ListTag bindingTags = tag.getList("anchor_bindings", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < bindingTags.size(); i++) {
            CompoundTag binding = bindingTags.getCompound(i);
            data.playerAnchors.computeIfAbsent(binding.getUUID("player_id"), ignored -> new ArrayList<>())
                    .add(binding.getString("anchor_id"));
        }
        ListTag cooldownTags = tag.getList("anchor_cooldowns", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < cooldownTags.size(); i++) {
            CompoundTag cooldown = cooldownTags.getCompound(i);
            data.anchorCooldowns.put(cooldown.getUUID("player_id"), cooldown.getLong("ready_at"));
        }
        ListTag respawnTags = tag.getList("pending_respawns", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < respawnTags.size(); i++) {
            CompoundTag respawn = respawnTags.getCompound(i);
            data.pendingRespawns.put(respawn.getUUID("player_id"), respawn.getString("anchor_id"));
        }
        data.totalChunks = tag.getInt("total_chunks");
        data.groupChunks = tag.contains("group_chunks")
                ? WorldDimensions.validateGroupChunks(tag.getInt("group_chunks"))
                : WorldDimensions.DEFAULT_GROUP_CHUNKS;
        if (tag.contains("generated_chunk_bits")) {
            data.generatedChunkBits.or(BitSet.valueOf(tag.getLongArray("generated_chunk_bits")));
        }
        data.pregenerationEnabled = tag.getBoolean("pregeneration_enabled");
        // Before this field existed, worlds were generated automatically. Preserve that layout until an admin enables editing.
        data.manualGroups = tag.contains("manual_groups") && tag.getBoolean("manual_groups");
        data.groupRevision = tag.getLong("group_revision");
        data.worldSeed = tag.getLong("world_seed");
        data.archipelagoSpawnApplied = tag.contains("archipelago_spawn_applied")
                ? tag.getBoolean("archipelago_spawn_applied")
                : !data.tiles.isEmpty();
        return data;
    }
}
