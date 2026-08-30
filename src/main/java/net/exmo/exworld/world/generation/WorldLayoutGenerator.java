package net.exmo.exworld.world.generation;

import net.exmo.exworld.world.model.Region;
import net.exmo.exworld.world.model.WorldBiome;
import net.exmo.exworld.world.model.WorldTile;
import net.exmo.exworld.world.model.WorldDimensions;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/** Generates one finite square atlas with connected, irregular regions and biome patches of different scales. */
public final class WorldLayoutGenerator {
    private static final int DIAMETER = WorldDimensions.MAP_SIZE;
    private static final int SITE_GROUP_COUNT = 96;
    private static final String[] PREFIXES = {"雾隐", "赤沙", "苍木", "白石", "星落", "暮潮", "风鸣", "旧都", "青穗", "北辰"};
    private static final String[] SUFFIXES = {"驿", "原", "谷", "港", "城", "泽", "岭", "墟", "道", "乡"};
    private static final String[] LANDMARKS = {"废弃车站", "边境村落", "古代哨塔", "旅行商栈", "地下遗迹", "猎人营地", "石桥", "旧矿井"};

    private WorldLayoutGenerator() {}

    public static GeneratedLayout generate(long seed) {
        return generate(seed, WorldDimensions.DEFAULT_GROUP_CHUNKS);
    }

    public static GeneratedLayout generate(long seed, int groupChunks) {
        return generate(seed, groupChunks, true);
    }

    /** Generates either the legacy biome/site groups or the manual-mode singleton starting partition. */
    public static GeneratedLayout generate(long seed, int groupChunks, boolean automaticGroups) {
        int groupBlocks = WorldDimensions.groupBlocks(groupChunks);
        Random random = new Random(seed ^ 0x45_58_57_4FL);
        WorldBiome[][] biomeMap = growBiomes(random);
        String[][] sites = placeSiteGroups(random);
        GroupLayout groups = buildGroups(random, biomeMap, sites);
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
                String site = sites[gz][gx] == null ? "暂无已知据点" : sites[gz][gx];
                String description = biome.displayName() + "延伸至此，" + biomeDescription(biome, seed, x, z);
                tiles.add(new WorldTile(tileId, x, z, region.id, region.name, biome.mapColor(),
                        WorldDimensions.groupCenter(x, groupChunks), WorldDimensions.groupCenter(z, groupChunks),
                        x == 0 && z == 0,
                        biome.id(), description, site, biome.resources()));
            }
        }

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

    /** Large-spread profiles win broad belts; small-spread profiles remain compact local patches. */
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
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!frontier.isEmpty()) {
            Frontier current = frontier.poll();
            if (owner[current.z][current.x] >= 0) continue;
            owner[current.z][current.x] = current.owner;
            for (int[] direction : directions) {
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

    /** Natural groups consume one connected biome patch; sites reserve their own additional one-tile groups. */
    private static GroupLayout buildGroups(Random random, WorldBiome[][] biomes, String[][] sites) {
        int[][] owners = emptyOwner();
        Map<Integer, RegionDraft> drafts = new LinkedHashMap<>();
        int nextOwner = 0;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int z = 0; z < DIAMETER; z++) {
            for (int x = 0; x < DIAMETER; x++) {
                if (owners[z][x] >= 0) continue;
                int owner = nextOwner++;
                boolean siteGroup = sites[z][x] != null;
                String id = (siteGroup ? "site_group_" : "biome_group_") + owner;
                String name = siteGroup ? sites[z][x]
                        : PREFIXES[random.nextInt(PREFIXES.length)] + SUFFIXES[random.nextInt(SUFFIXES.length)];
                drafts.put(owner, new RegionDraft(id, name, random.nextLong()));
                if (siteGroup) {
                    owners[z][x] = owner;
                    continue;
                }
                WorldBiome biome = biomes[z][x];
                ArrayDeque<Cell> queue = new ArrayDeque<>();
                queue.add(new Cell(x, z));
                owners[z][x] = owner;
                while (!queue.isEmpty()) {
                    Cell current = queue.removeFirst();
                    for (int[] direction : directions) {
                        int nx = current.x + direction[0];
                        int nz = current.z + direction[1];
                        if (nx < 0 || nz < 0 || nx >= DIAMETER || nz >= DIAMETER || owners[nz][nx] >= 0
                                || sites[nz][nx] != null || biomes[nz][nx] != biome) continue;
                        owners[nz][nx] = owner;
                        queue.add(new Cell(nx, nz));
                    }
                }
            }
        }
        return new GroupLayout(owners, drafts);
    }

    private static String[][] placeSiteGroups(Random random) {
        String[][] sites = new String[DIAMETER][DIAMETER];
        List<Cell> seeds = spacedSeeds(random, SITE_GROUP_COUNT, 5);
        for (int index = 0; index < seeds.size(); index++) {
            Cell cell = seeds.get(index);
            sites[cell.z][cell.x] = LANDMARKS[index % LANDMARKS.length];
        }
        return sites;
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
