package net.exmo.exworld.world.generation;

import net.exmo.exworld.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared inverted-pyramid island field used by the density function, spawn placement and the strategic map.
 * Cell size is the midpoint of the configured 200–300 spacing so neighbouring centres stay in that band after jitter.
 */
public final class IslandLayout {
    public static final int DEFAULT_SPACING_MIN = 200;
    public static final int DEFAULT_SPACING_MAX = 300;
    public static final int DEFAULT_TOP_RADIUS_MIN = 32;
    public static final int DEFAULT_TOP_RADIUS_MAX = 72;
    public static final int DEFAULT_THICKNESS_MIN = 12;
    public static final int DEFAULT_THICKNESS_MAX = 28;
    public static final double DEFAULT_TAPER = 0.35;
    public static final int DEFAULT_MIN_Y = 120;
    public static final int DEFAULT_MAX_Y = 220;

    private static volatile long worldSeed;
    private static volatile Settings boundSettings = Settings.defaults();

    private IslandLayout() {}

    public static void bindSeed(long seed) {
        worldSeed = seed;
        boundSettings = Settings.current();
    }
    public static long worldSeed() { return worldSeed; }
    public static Settings boundSettings() { return boundSettings; }

    public static Island originIsland(long seed) {
        return originIsland(seed, boundSettings);
    }

    public static Island originIsland(long seed, Settings settings) {
        return islandInCell(seed, settings, 0, 0);
    }

    public static double density(long seed, int x, int y, int z) {
        return density(seed, boundSettings, x, y, z);
    }

    public static double density(long seed, Settings settings, int x, int y, int z) {
        int spacing = settings.spacing();
        int cellX = cellIndex(x, spacing);
        int cellZ = cellIndex(z, spacing);
        double best = -1.0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                Island island = islandInCell(seed, settings, cellX + dx, cellZ + dz);
                if (island == null) continue;
                best = Math.max(best, islandDensity(island, seed, x, y, z));
            }
        }
        return best;
    }

    public static Island islandInCell(long seed, Settings settings, int cellX, int cellZ) {
        boolean origin = cellX == 0 && cellZ == 0;
        int spacing = settings.spacing();
        int centerX;
        int centerZ;
        if (origin) {
            centerX = 0;
            centerZ = 0;
        } else {
            double jitterX = (unit(seed, cellX, cellZ, 23) * 2.0 - 1.0) * settings.jitterAmplitude();
            double jitterZ = (unit(seed, cellX, cellZ, 29) * 2.0 - 1.0) * settings.jitterAmplitude();
            centerX = (int) Math.round(cellX * (double) spacing + jitterX);
            centerZ = (int) Math.round(cellZ * (double) spacing + jitterZ);
        }
        Kind kind = origin ? Kind.MAIN : kind(unit(seed, cellX, cellZ, 41));
        double radiusUnit = origin ? 0.82 : unit(seed, cellX, cellZ, 47);
        double topRadius = settings.topRadiusMin + radiusUnit * (settings.topRadiusMax - settings.topRadiusMin);
        if (origin) topRadius = Math.max(topRadius, (settings.topRadiusMin + settings.topRadiusMax) * 0.5);
        int thickness = settings.thicknessMin + (int) Math.round(unit(seed, cellX, cellZ, 53)
                * (settings.thicknessMax - settings.thicknessMin));
        int topY = settings.minY + (int) Math.round(unit(seed, cellX, cellZ, 59) * (settings.maxY - settings.minY));
        if (origin) topY = Math.max(topY, (settings.minY + settings.maxY) / 2);
        return new Island(cellX, cellZ, centerX, centerZ, topY, topRadius, thickness, settings.taper, kind);
    }

    public static Island nearestIsland(long seed, Settings settings, int worldX, int worldZ) {
        int cellX = cellIndex(worldX, settings.spacing());
        int cellZ = cellIndex(worldZ, settings.spacing());
        Island best = islandInCell(seed, settings, cellX, cellZ);
        double bestDist = Math.hypot(worldX - best.centerX(), worldZ - best.centerZ());
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0) continue;
                Island candidate = islandInCell(seed, settings, cellX + dx, cellZ + dz);
                double dist = Math.hypot(worldX - candidate.centerX(), worldZ - candidate.centerZ());
                if (dist < bestDist) {
                    best = candidate;
                    bestDist = dist;
                }
            }
        }
        return best;
    }

    public static List<Island> islandsOverlappingBlocks(long seed, Settings settings, int minX, int minZ, int maxX, int maxZ) {
        int spacing = settings.spacing();
        int minCellX = cellIndex(minX, spacing) - 1;
        int maxCellX = cellIndex(maxX, spacing) + 1;
        int minCellZ = cellIndex(minZ, spacing) - 1;
        int maxCellZ = cellIndex(maxZ, spacing) + 1;
        List<Island> islands = new ArrayList<>();
        for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                Island island = islandInCell(seed, settings, cellX, cellZ);
                if (island == null) continue;
                if (island.centerX() < minX - island.topRadius() || island.centerX() > maxX + island.topRadius()) continue;
                if (island.centerZ() < minZ - island.topRadius() || island.centerZ() > maxZ + island.topRadius()) continue;
                islands.add(island);
            }
        }
        return islands;
    }

    public static int cellIndex(int coordinate, int spacing) {
        return Math.floorDiv(coordinate + spacing / 2, spacing);
    }

    public static int spacingBetween(Island a, Island b) {
        int dx = a.centerX() - b.centerX();
        int dz = a.centerZ() - b.centerZ();
        return (int) Math.round(Math.hypot(dx, dz));
    }

    private static double islandDensity(Island island, long seed, int x, int y, int z) {
        int bottom = island.topY() - island.thickness();
        if (y > island.topY() || y < bottom) return -1.0;
        double t = island.thickness() <= 0 ? 0.0 : (island.topY() - y) / (double) island.thickness();
        double radius = island.topRadius() * (1.0 - t * island.taper());
        double dist = Math.hypot(x - island.centerX(), z - island.centerZ());
        double mask = (radius - dist) / Math.max(1.0, radius);
        double roughness = (unit(seed, x * 3 + island.cellX(), z * 5 + island.cellZ(), 71 + y) - 0.5) * 0.12;
        double density = mask + roughness;
        if (density <= 0.0) return -0.45;
        return density * 0.7;
    }

    private static Kind kind(double unit) {
        if (unit < 0.05) return Kind.MAIN;
        if (unit < 0.25) return Kind.RESOURCE;
        if (unit < 0.60) return Kind.NORMAL;
        if (unit < 0.65) return Kind.SECRET;
        if (unit < 0.80) return Kind.DEAD;
        return Kind.WANDERING;
    }

    public static double unit(long seed, int x, int z, int salt) {
        long mixed = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ salt;
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return (mixed >>> 11) * 0x1.0p-53;
    }

    public record Settings(int spacingMin, int spacingMax, int topRadiusMin, int topRadiusMax, int thicknessMin,
                           int thicknessMax, double taper, int minY, int maxY) {
        public static Settings defaults() {
            return new Settings(DEFAULT_SPACING_MIN, DEFAULT_SPACING_MAX, DEFAULT_TOP_RADIUS_MIN, DEFAULT_TOP_RADIUS_MAX,
                    DEFAULT_THICKNESS_MIN, DEFAULT_THICKNESS_MAX, DEFAULT_TAPER, DEFAULT_MIN_Y, DEFAULT_MAX_Y);
        }

        public static Settings current() {
            return new Settings(Config.islandSpacingMin, Config.islandSpacingMax, DEFAULT_TOP_RADIUS_MIN,
                    DEFAULT_TOP_RADIUS_MAX, DEFAULT_THICKNESS_MIN, DEFAULT_THICKNESS_MAX, DEFAULT_TAPER,
                    DEFAULT_MIN_Y, DEFAULT_MAX_Y);
        }

        public int spacing() {
            int min = Math.min(spacingMin, spacingMax);
            int max = Math.max(spacingMin, spacingMax);
            return Math.max(16, (min + max) / 2);
        }

        public double jitterAmplitude() {
            int min = Math.min(spacingMin, spacingMax);
            int max = Math.max(spacingMin, spacingMax);
            return Math.max(0.0, (max - min) / 4.0);
        }
    }

    public record Island(int cellX, int cellZ, int centerX, int centerZ, int topY, double topRadius, int thickness,
                         double taper, Kind kind) {
        public String mapLabel() { return kind.mapLabel(); }
        public boolean named() { return kind != Kind.NORMAL; }
    }

    public enum Kind {
        MAIN("主岛"),
        RESOURCE("资源岛"),
        NORMAL("浮岛"),
        SECRET("秘境岛"),
        DEAD("死岛"),
        WANDERING("游岛");

        private final String mapLabel;
        Kind(String mapLabel) { this.mapLabel = mapLabel; }
        public String mapLabel() { return mapLabel; }
    }
}
