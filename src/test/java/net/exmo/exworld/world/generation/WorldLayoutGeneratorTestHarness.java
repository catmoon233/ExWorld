package net.exmo.exworld.world.generation;

import net.exmo.exworld.world.model.Region;
import net.exmo.exworld.world.model.WorldTile;
import net.exmo.exworld.world.model.WorldDimensions;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Property harness for atlas size, biome zones, island halos and the manual singleton partition. */
public final class WorldLayoutGeneratorTestHarness {
    public static void main(String[] args) {
        var layout = WorldLayoutGenerator.generate(0x4558574F524C44L, WorldDimensions.DEFAULT_GROUP_CHUNKS, true, 48);
        require(layout.tiles().size() == WorldDimensions.MAP_SIZE * WorldDimensions.MAP_SIZE,
                "atlas must fill the 128x128 finite square");
        require(layout.regions().stream().noneMatch(net.exmo.exworld.world.model.Region::configured),
                "automatic groups must start unconfigured so the M map stays biome-only until edited");
        require(layout.regions().stream().allMatch(region -> region.id().startsWith("biome_group_")),
                "automatic layout must keep islands inside biome zones instead of splitting site groups");
        Map<String, WorldTile> byId = new HashMap<>();
        layout.tiles().forEach(tile -> byId.put(tile.id(), tile));
        boolean irregularRegion = false;
        boolean namedZone = false;
        WorldTile origin = byCoordinate(layout.tiles(), 0, 0);
        require(origin != null && !origin.sites().equals("暂无已知据点"), "origin tile must be labelled as the main island");
        for (Region region : layout.regions()) {
            require(connected(region.tileIds(), byId), "region must be connected: " + region.id());
            if (region.name().contains("区") || region.name().contains("诸天")) namedZone = true;
            WorldTile firstTile = byId.get(region.tileIds().getFirst());
            for (String tileId : region.tileIds()) {
                WorldTile tile = byId.get(tileId);
                require(tile.biomeId().equals(firstTile.biomeId()), "natural group crosses biome patches");
            }
            int minX = region.tileIds().stream().map(byId::get).mapToInt(WorldTile::mapX).min().orElseThrow();
            int maxX = region.tileIds().stream().map(byId::get).mapToInt(WorldTile::mapX).max().orElseThrow();
            int minZ = region.tileIds().stream().map(byId::get).mapToInt(WorldTile::mapZ).min().orElseThrow();
            int maxZ = region.tileIds().stream().map(byId::get).mapToInt(WorldTile::mapZ).max().orElseThrow();
            if (region.tileIds().size() < (maxX - minX + 1) * (maxZ - minZ + 1)) irregularRegion = true;
        }
        require(namedZone, "at least one zone should use the 诸天/区 naming band");
        long seed = 0x4558574F524C44L;
        int groupChunks = WorldDimensions.DEFAULT_GROUP_CHUNKS;
        IslandLayout.Settings settings = IslandLayout.boundSettings();
        int minBlock = WorldDimensions.groupCenter(WorldDimensions.MAP_MIN, groupChunks)
                - WorldDimensions.groupBlocks(groupChunks) / 2;
        int maxBlock = WorldDimensions.groupCenter(WorldDimensions.MAP_MAX_EXCLUSIVE - 1, groupChunks)
                + WorldDimensions.groupBlocks(groupChunks) / 2;
        for (IslandLayout.Island island : IslandLayout.islandsOverlappingBlocks(seed, settings, minBlock, minBlock,
                maxBlock, maxBlock)) {
            int mapX = WorldDimensions.groupCoordinate(island.centerX(), groupChunks);
            int mapZ = WorldDimensions.groupCoordinate(island.centerZ(), groupChunks);
            WorldTile center = byCoordinate(layout.tiles(), mapX, mapZ);
            if (center == null || center.sites().equals("暂无已知据点")) continue;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    WorldTile neighbor = byCoordinate(layout.tiles(), mapX + dx, mapZ + dz);
                    if (neighbor == null || neighbor.regionId().equals(center.regionId())) continue;
                    IslandLayout.Island hosted = IslandLayout.nearestIsland(seed, settings, neighbor.worldX(),
                            neighbor.worldZ());
                    boolean hostsOwn = WorldDimensions.groupCoordinate(hosted.centerX(), groupChunks) == neighbor.mapX()
                            && WorldDimensions.groupCoordinate(hosted.centerZ(), groupChunks) == neighbor.mapZ();
                    require(!neighbor.sites().equals(center.sites()) || hostsOwn,
                            "island halo leaked across zone at " + neighbor.id());
                }
            }
        }
        var tight = WorldLayoutGenerator.generate(seed, groupChunks, true, 8);
        require(tight.regions().size() > layout.regions().size(),
                "smaller zoneTargetSpan must split the atlas into more named zones");
        require(irregularRegion, "at least one region must have a non-rectangular outline");
        Map<String, Long> biomeSizes = layout.tiles().stream().collect(java.util.stream.Collectors.groupingBy(
                WorldTile::biomeId, java.util.stream.Collectors.counting()));
        require(biomeSizes.size() >= 6, "world needs biome variety");
        long largest = biomeSizes.values().stream().mapToLong(Long::longValue).max().orElseThrow();
        long smallest = biomeSizes.values().stream().mapToLong(Long::longValue).min().orElseThrow();
        require(largest >= smallest * 2, "biome profiles must create different patch scales");
        var manual = WorldLayoutGenerator.generate(0x4558574F524C44L, 4, false);
        require(manual.regions().size() == WorldDimensions.MAP_SIZE * WorldDimensions.MAP_SIZE,
                "manual mode must begin with one editable group per world tile");
        require(manual.regions().stream().allMatch(region -> region.tileIds().size() == 1),
                "manual mode must not generate automatic multi-tile groups");
        require(manual.regions().stream().noneMatch(net.exmo.exworld.world.model.Region::configured),
                "manual singleton groups must remain hidden from M-map group overlays until configured");
        System.out.println("WORLD_LAYOUT_TEST_OK tiles=" + layout.tiles().size() + " regions="
                + layout.regions().size() + " biomes=" + biomeSizes);
    }

    private static WorldTile byCoordinate(List<WorldTile> tiles, int x, int z) {
        if (x < WorldDimensions.MAP_MIN || x >= WorldDimensions.MAP_MAX_EXCLUSIVE
                || z < WorldDimensions.MAP_MIN || z >= WorldDimensions.MAP_MAX_EXCLUSIVE) return null;
        return tiles.get((z - WorldDimensions.MAP_MIN) * WorldDimensions.MAP_SIZE + x - WorldDimensions.MAP_MIN);
    }

    private static boolean connected(List<String> ids, Map<String, WorldTile> byId) {
        Set<String> remaining = new HashSet<>(ids);
        ArrayDeque<WorldTile> queue = new ArrayDeque<>();
        WorldTile first = byId.get(ids.getFirst());
        queue.add(first);
        remaining.remove(first.id());
        while (!queue.isEmpty()) {
            WorldTile tile = queue.removeFirst();
            for (String id : ids) {
                WorldTile candidate = byId.get(id);
                if (remaining.contains(id) && Math.abs(tile.mapX() - candidate.mapX())
                        + Math.abs(tile.mapZ() - candidate.mapZ()) == 1) {
                    remaining.remove(id);
                    queue.add(candidate);
                }
            }
        }
        return remaining.isEmpty();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
