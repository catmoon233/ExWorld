package net.exmo.exworld.world.generation;

import net.exmo.exworld.Config;
import net.exmo.exworld.world.model.Region;
import net.exmo.exworld.world.model.WorldBiome;
import net.exmo.exworld.world.model.WorldTile;
import net.exmo.exworld.world.model.WorldDimensions;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/** Generates one finite square atlas with connected biome zones and island sites that stay inside those zones. */
public final class WorldLayoutGenerator {
    private static final int DIAMETER = WorldDimensions.MAP_SIZE;
    private static final String[] PREFIXES = {"诸天", "雾隐", "赤沙", "苍木", "白石", "星落", "暮潮", "风鸣", "浮穹", "北辰"};
    private static final String[] SUFFIXES = {"区", "驿", "原", "谷", "界", "泽", "岭", "墟", "道", "乡"};
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private WorldLayoutGenerator() {}

    public static GeneratedLayout generate(long seed) {
        return generate(seed, WorldDimensions.DEFAULT_GROUP_CHUNKS);
    }

    public static GeneratedLayout generate(long seed, int groupChunks) {
        return generate(seed, groupChunks, true);
    }

    public static GeneratedLayout generate(long seed, int groupChunks, boolean automaticGroups) {
        return generate(seed, groupChunks, automaticGroups, Config.zoneTargetSpan);
    }

    /** Generates either biome zones (with island halos) or the manual-mode singleton starting partition. */
    public static GeneratedLayout generate(long seed, int groupChunks, boolean automaticGroups, int zoneTargetSpan) {
        Random random = new Random(seed ^ 0x45_58_57_4FL);
        WorldBiome[][] biomeMap = growBiomes(random);
        GroupLayout groups = buildGroups(random, biomeMap, Math.max(8, zoneTargetSpan));
        int[][] regionMap = groups.owners;
        Map<Integer, RegionDraft> drafts = groups.drafts;
        List<WorldTile> tiles = new ArrayList<>(DIAMETER * DIAMETER);

        for (int gz = 0; gz < DIAMETER; gz++) {
            for (int gx = 0; gx < DIAMETER; gx++) {
                int x = gx + WorldDimensions.MAP_MIN;
                int z = gz + WorldDimensions.MAP_MIN;
                int regionIndex = regionMap[gz][gx];
                RegionDraft region = drafts.get(regionIndex);
                WorldBiome biome = biomeMap[gz][gx];
                String tileId = "tile_" + signed(x) + "_" + signed(z);
                region.tileIds.add(tileId);
                String description = biome.displayName() + "延伸至此，" + biomeDescription(biome, seed, x, z);
                tiles.add(new WorldTile(tileId, x, z, region.id, region.name, biome.mapColor(),
                        WorldDimensions.groupCenter(x, groupChunks), WorldDimensions.groupCenter(z, groupChunks),
                        x == 0 && z == 0,
                        biome.id(), description, "暂无已知据点", biome.resources()));
            }
        }

        overlayIslands(tiles, seed, groupChunks);

        List<Region> regions = drafts.values().stream()
                .map(region -> new Region(region.id, region.tileIds, region.name, region.storySeed)).toList();
        GeneratedLayout generated = new GeneratedLayout(tiles, regions);
        if (automaticGroups) return generated;
        List<WorldTile> manualTiles = tiles.stream().map(tile -> new WorldTile(tile.id(), tile.mapX(), tile.mapZ(),
                "manual_" + tile.id(), tile.name(), tile.color(), tile.worldX(), tile.worldZ(), tile.discovered(),
                tile.biomeId(), tile.description(), tile.sites(), tile.resources())).toList();
        List<Region> manualRegions = manualTiles.stream().map(tile -> new Region(tile.regionId(), List.of(tile.id()),
                tile.name() + " [" + tile.mapX() + ", " + tile.mapZ() + "]", seed ^ tile.id().hashCode())).toList();
        return new GeneratedLayout(manualTiles, manualRegions);
    }

    private static void overlayIslands(List<WorldTile> tiles, long seed, int groupChunks) {
        Map<Long, Integer> indexByCoord = new HashMap<>();
        for (int i = 0; i < tiles.size(); i++) {
            WorldTile tile = tiles.get(i);
            indexByCoord.put(pack(tile.mapX(), tile.mapZ()), i);
        }
        int minBlock = WorldDimensions.groupCenter(WorldDimensions.MAP_MIN, groupChunks)
                - WorldDimensions.groupBlocks(groupChunks) / 2;
        int maxBlock = WorldDimensions.groupCenter(WorldDimensions.MAP_MAX_EXCLUSIVE - 1, groupChunks)
                + WorldDimensions.groupBlocks(groupChunks) / 2;
        IslandLayout.Settings settings = IslandLayout.boundSettings();
        for (IslandLayout.Island island : IslandLayout.islandsOverlappingBlocks(seed, settings, minBlock, minBlock,
                maxBlock, maxBlock)) {
            if (!island.named() && IslandLayout.unit(seed, island.cellX(), island.cellZ(), 83) > 0.18) continue;
            int mapX = WorldDimensions.groupCoordinate(island.centerX(), groupChunks);
            int mapZ = WorldDimensions.groupCoordinate(island.centerZ(), groupChunks);
            Integer centerIndex = indexByCoord.get(pack(mapX, mapZ));
            if (centerIndex == null) continue;
            WorldTile center = tiles.get(centerIndex);
            String label = island.mapLabel();
            tiles.set(centerIndex, new WorldTile(center.id(), center.mapX(), center.mapZ(), center.regionId(), center.name(),
                    center.color(), center.worldX(), center.worldZ(), center.discovered(), center.biomeId(),
                    center.description(), label, center.resources()));
        }
    }

    private static long pack(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }

    private static WorldBiome[][] growBiomes(Random random) {
        WorldBiome[] seedProfiles = biomeSeedProfiles();
        int[][] owner = emptyOwner();
        PriorityQueue<Frontier> frontier = new PriorityQueue<>(Comparator.comparingDouble(Frontier::cost));
        List<Cell> seeds = spacedSeeds(random, seedProfiles.length, 2);
        for (int i = 0; i < seeds.size(); i++) {
            Cell cell = seeds.get(i);
            frontier.add(new Frontier(cell.x, cell.z, i, 0.0));
        }
        grow(owner, frontier, 0x42494F4D4553L, seedProfiles);
        WorldBiome[][] result = new WorldBiome[DIAMETER][DIAMETER];
        for (int z = 0; z < DIAMETER; z++) for (int x = 0; x < DIAMETER; x++) result[z][x] = seedProfiles[owner[z][x]];
        return result;
    }

    private static WorldBiome[] biomeSeedProfiles() {
        List<WorldBiome> profiles = new ArrayList<>(128);
        addProfiles(profiles, WorldBiome.PRAIRIE, 34);
        addProfiles(profiles, WorldBiome.FOREST, 28);
        addProfiles(profiles, WorldBiome.DESERT, 16);
        addProfiles(profiles, WorldBiome.TAIGA, 14);
        addProfiles(profiles, WorldBiome.HIGHLANDS, 13);
        addProfiles(profiles, WorldBiome.BADLANDS, 9);
        addProfiles(profiles, WorldBiome.SWAMP, 8);
        addProfiles(profiles, WorldBiome.FLOWER_FIELDS, 6);
        return profiles.toArray(WorldBiome[]::new);
    }

    private static void addProfiles(List<WorldBiome> profiles, WorldBiome biome, int count) {
        for (int i = 0; i < count; i++) profiles.add(biome);
    }

    private static void grow(int[][] owner, PriorityQueue<Frontier> frontier, long salt, WorldBiome[] profiles) {
        while (!frontier.isEmpty()) {
            Frontier current = frontier.poll();
            if (owner[current.z][current.x] >= 0) continue;
            owner[current.z][current.x] = current.owner;
            for (int[] direction : DIRECTIONS) {
                int nx = current.x + direction[0];
                int nz = current.z + direction[1];
                if (nx < 0 || nz < 0 || nx >= DIAMETER || nz >= DIAMETER || owner[nz][nx] >= 0) continue;
                double irregularity = 0.72 + deterministicUnit(salt + current.owner * 31L, nx, nz,
                        direction[0] * 7 + direction[1] * 13) * 0.7;
                double profileCost = profiles == null ? 1.0 : 1.0 / profiles[current.owner].spread();
                frontier.add(new Frontier(nx, nz, current.owner, current.cost + irregularity * profileCost));
            }
        }
    }

    private static GroupLayout buildGroups(Random random, WorldBiome[][] biomes, int zoneTargetSpan) {
        int[][] owners = emptyOwner();
        Map<Integer, RegionDraft> drafts = new LinkedHashMap<>();
        int nextOwner = 0;
        boolean[][] visited = new boolean[DIAMETER][DIAMETER];
        for (int z = 0; z < DIAMETER; z++) {
            for (int x = 0; x < DIAMETER; x++) {
                if (visited[z][x]) continue;
                List<Cell> patch = floodBiome(biomes, visited, x, z);
                for (List<Cell> zone : partitionPatch(patch, zoneTargetSpan)) {
                    int owner = nextOwner++;
                    String name = PREFIXES[random.nextInt(PREFIXES.length)] + SUFFIXES[random.nextInt(SUFFIXES.length)];
                    drafts.put(owner, new RegionDraft("biome_group_" + owner, name, random.nextLong()));
                    for (Cell cell : zone) owners[cell.z][cell.x] = owner;
                }
            }
        }
        return new GroupLayout(owners, drafts);
    }

    private static List<Cell> floodBiome(WorldBiome[][] biomes, boolean[][] visited, int startX, int startZ) {
        WorldBiome biome = biomes[startZ][startX];
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        List<Cell> patch = new ArrayList<>();
        queue.add(new Cell(startX, startZ));
        visited[startZ][startX] = true;
        while (!queue.isEmpty()) {
            Cell current = queue.removeFirst();
            patch.add(current);
            for (int[] direction : DIRECTIONS) {
                int nx = current.x + direction[0];
                int nz = current.z + direction[1];
                if (nx < 0 || nz < 0 || nx >= DIAMETER || nz >= DIAMETER || visited[nz][nx]
                        || biomes[nz][nx] != biome) continue;
                visited[nz][nx] = true;
                queue.add(new Cell(nx, nz));
            }
        }
        return patch;
    }

    private static List<List<Cell>> partitionPatch(List<Cell> patch, int zoneTargetSpan) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        boolean[][] inPatch = new boolean[DIAMETER][DIAMETER];
        for (Cell cell : patch) {
            inPatch[cell.z][cell.x] = true;
            minX = Math.min(minX, cell.x);
            maxX = Math.max(maxX, cell.x);
            minZ = Math.min(minZ, cell.z);
            maxZ = Math.max(maxZ, cell.z);
        }
        if (maxX - minX < zoneTargetSpan && maxZ - minZ < zoneTargetSpan) return List.of(patch);
        List<Cell> seeds = new ArrayList<>();
        for (int z = minZ; z <= maxZ; z += zoneTargetSpan) {
            for (int x = minX; x <= maxX; x += zoneTargetSpan) {
                Cell seed = nearestPatchCell(inPatch, x, z, minX, maxX, minZ, maxZ);
                if (seed != null && seeds.stream().noneMatch(existing -> existing.x == seed.x && existing.z == seed.z)) {
                    seeds.add(seed);
                }
            }
        }
        if (seeds.size() <= 1) return List.of(patch);
        int[][] owner = emptyOwner();
        ArrayDeque<Frontier> queue = new ArrayDeque<>();
        for (int i = 0; i < seeds.size(); i++) {
            Cell seed = seeds.get(i);
            owner[seed.z][seed.x] = i;
            queue.add(new Frontier(seed.x, seed.z, i, 0));
        }
        while (!queue.isEmpty()) {
            Frontier current = queue.removeFirst();
            for (int[] direction : DIRECTIONS) {
                int nx = current.x + direction[0];
                int nz = current.z + direction[1];
                if (nx < 0 || nz < 0 || nx >= DIAMETER || nz >= DIAMETER || !inPatch[nz][nx] || owner[nz][nx] >= 0) continue;
                owner[nz][nx] = current.owner;
                queue.add(new Frontier(nx, nz, current.owner, 0));
            }
        }
        List<List<Cell>> zones = new ArrayList<>();
        for (int i = 0; i < seeds.size(); i++) zones.add(new ArrayList<>());
        List<Cell> leftovers = new ArrayList<>();
        for (Cell cell : patch) {
            int assigned = owner[cell.z][cell.x];
            if (assigned >= 0) zones.get(assigned).add(cell);
            else leftovers.add(cell);
        }
        for (Cell leftover : leftovers) {
            List<Cell> extra = new ArrayList<>();
            extra.add(leftover);
            zones.add(extra);
        }
        zones.removeIf(List::isEmpty);
        return zones;
    }

    private static Cell nearestPatchCell(boolean[][] inPatch, int x, int z, int minX, int maxX, int minZ, int maxZ) {
        Cell best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int pz = minZ; pz <= maxZ; pz++) {
            for (int px = minX; px <= maxX; px++) {
                if (!inPatch[pz][px]) continue;
                int dist = Math.abs(px - x) + Math.abs(pz - z);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new Cell(px, pz);
                    if (dist == 0) return best;
                }
            }
        }
        return best;
    }

    private static List<Cell> spacedSeeds(Random random, int count, int minimumDistance) {
        List<Cell> seeds = new ArrayList<>();
        int attempts = 0;
        while (seeds.size() < count && attempts++ < count * 200) {
            Cell candidate = new Cell(random.nextInt(DIAMETER), random.nextInt(DIAMETER));
            if (seeds.stream().allMatch(seed -> Math.abs(seed.x - candidate.x) + Math.abs(seed.z - candidate.z) >= minimumDistance)) {
                seeds.add(candidate);
            }
        }
        for (int z = 0; seeds.size() < count && z < DIAMETER; z++) {
            for (int x = 0; seeds.size() < count && x < DIAMETER; x++) {
                Cell candidate = new Cell(x, z);
                if (!seeds.contains(candidate)) seeds.add(candidate);
            }
        }
        return seeds;
    }

    private static int[][] emptyOwner() {
        int[][] owner = new int[DIAMETER][DIAMETER];
        for (int[] row : owner) Arrays.fill(row, -1);
        return owner;
    }

    private static String biomeDescription(WorldBiome biome, long seed, int x, int z) {
        String[] endings = switch (biome) {
            case PRAIRIE -> new String[]{"长风压低草浪，商路沿缓坡向远方展开。", "水草丰润，成群野兽在低丘间迁徙。"};
            case FOREST -> new String[]{"树冠遮蔽旧路，林下散落着潮湿苔石。", "林木密集，只有猎人留下的细径仍可辨认。"};
            case SWAMP -> new String[]{"浅水与泥滩交错，雾气长久停留在芦苇间。", "腐殖土覆盖低地，隐秘水道时常改变方向。"};
            case DESERT -> new String[]{"沙丘吞没古道，裸露岩层记录着干涸河床。", "热风卷过砂砾，水源只在背阴地短暂停留。"};
            case BADLANDS -> new String[]{"赤色岩壁层叠抬升，矿脉从裂谷中显露。", "风蚀柱林切开台地，狭窄谷道适合设伏。"};
            case TAIGA -> new String[]{"寒松覆盖缓坡，积雪保存着兽群足迹。", "针叶林间溪流冰冷，夜晚常传来远处嚎叫。"};
            case HIGHLANDS -> new String[]{"裸岩与碎石坡抬高地平线，风声几乎从不停止。", "山脊控制周围道路，也藏着难以开采的矿层。"};
            case FLOWER_FIELDS -> new String[]{"成片花田沿水脉生长，空气里有持久甜香。", "温和坡地聚集昆虫与药草，也吸引采集者驻留。"};
        };
        return endings[Math.floorMod(hash(seed, x, z, 17), endings.length)];
    }

    private static double deterministicUnit(long seed, int x, int z, int salt) {
        long mixed = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ salt;
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return (mixed >>> 11) * 0x1.0p-53;
    }

    private static int hash(long seed, int x, int z, int salt) {
        return (int) (deterministicUnit(seed, x, z, salt) * Integer.MAX_VALUE);
    }

    private static String signed(int value) { return value < 0 ? "n" + -value : "p" + value; }

    public record GeneratedLayout(List<WorldTile> tiles, List<Region> regions) {}
    private record GroupLayout(int[][] owners, Map<Integer, RegionDraft> drafts) {}
    private record Cell(int x, int z) {}
    private record Frontier(int x, int z, int owner, double cost) {}

    private static final class RegionDraft {
        private final String id;
        private final String name;
        private final long storySeed;
        private final List<String> tileIds = new ArrayList<>();
        private RegionDraft(String id, String name, long storySeed) { this.id = id; this.name = name; this.storySeed = storySeed; }
    }
}
