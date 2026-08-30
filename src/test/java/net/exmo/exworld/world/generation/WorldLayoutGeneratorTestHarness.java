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

/** Property harness for atlas size, irregular connected regions and varied biome patch sizes. */
public final class WorldLayoutGeneratorTestHarness {
    public static void main(String[] args) {
        var layout = WorldLayoutGenerator.generate(0x4558574F524C44L);
        require(layout.tiles().size() == WorldDimensions.MAP_SIZE * WorldDimensions.MAP_SIZE,
                "atlas must fill the 128x128 finite square");
        require(layout.regions().stream().noneMatch(net.exmo.exworld.world.model.Region::configured),
                "automatic groups must start unconfigured so the M map stays biome-only until edited");
        Map<String, WorldTile> byId = new HashMap<>();
        layout.tiles().forEach(tile -> byId.put(tile.id(), tile));
        boolean irregularRegion = false;
        boolean siteGroup = false;
        for (Region region : layout.regions()) {
            require(connected(region.tileIds(), byId), "region must be connected: " + region.id());
            WorldTile firstTile = byId.get(region.tileIds().getFirst());
            if (region.id().startsWith("site_group_")) {
                siteGroup = true;
                require(region.tileIds().size() == 1, "a generated site must reserve its own world-tile group");
                require(!firstTile.sites().equals("暂无已知据点"), "site group must carry a site");
            } else {
                require(region.id().startsWith("biome_group_"), "unknown generated group kind: " + region.id());
                for (String tileId : region.tileIds()) {
                    WorldTile tile = byId.get(tileId);
                    require(tile.biomeId().equals(firstTile.biomeId()), "natural group crosses biome patches");
                    require(tile.sites().equals("暂无已知据点"), "site tile leaked into natural group");
                }
            }
            int minX = region.tileIds().stream().map(byId::get).mapToInt(WorldTile::mapX).min().orElseThrow();
            int maxX = region.tileIds().stream().map(byId::get).mapToInt(WorldTile::mapX).max().orElseThrow();
            int minZ = region.tileIds().stream().map(byId::get).mapToInt(WorldTile::mapZ).min().orElseThrow();
            int maxZ = region.tileIds().stream().map(byId::get).mapToInt(WorldTile::mapZ).max().orElseThrow();
            if (region.tileIds().size() < (maxX - minX + 1) * (maxZ - minZ + 1)) irregularRegion = true;
        }
        require(siteGroup, "world must contain additional site groups");
        for (WorldTile tile : layout.tiles()) {
            if (!tile.sites().equals("暂无已知据点")) continue;
            for (int[] direction : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                WorldTile neighbor = byCoordinate(layout.tiles(), tile.mapX() + direction[0], tile.mapZ() + direction[1]);
                if (neighbor != null && neighbor.sites().equals("暂无已知据点")
                        && neighbor.biomeId().equals(tile.biomeId())) {
                    require(neighbor.regionId().equals(tile.regionId()),
                            "one connected biome patch was split across natural groups");
                }
            }
        }
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
