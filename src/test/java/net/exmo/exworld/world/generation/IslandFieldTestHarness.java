package net.exmo.exworld.world.generation;

import net.exmo.exworld.Config;
import net.exmo.exworld.world.model.WorldDimensions;

/** Property checks for inverted-pyramid islands, origin guarantee and 200–300 spacing. */
public final class IslandFieldTestHarness {
    public static void main(String[] args) {
        long seed = 0x4558574F524C44L;
        IslandLayout.Settings settings = IslandLayout.Settings.defaults();
        IslandLayout.Island origin = IslandLayout.originIsland(seed, settings);
        require(origin != null && origin.kind() == IslandLayout.Kind.MAIN, "origin cell must always host the main island");
        require(origin.centerX() == 0 && origin.centerZ() == 0, "main island must sit on world origin");
        require(origin.topRadius() >= settings.topRadiusMin() && origin.thickness() >= settings.thicknessMin(),
                "origin island is too small to spawn on");
        require(IslandLayout.density(seed, settings, 0, origin.topY() - 2, 0) > 0,
                "origin island interior must be solid");
        require(IslandLayout.density(seed, settings, 0, origin.topY() + 4, 0) < 0,
                "air above the flat top must stay empty");
        require(IslandLayout.density(seed, settings, 0, origin.topY() - origin.thickness() - 2, 0) < 0,
                "air below the tapered keel must stay empty");

        double topRadiusDensity = IslandLayout.density(seed, settings, (int) (origin.topRadius() * 0.2), origin.topY() - 1, 0);
        double keelRadiusDensity = IslandLayout.density(seed, settings, (int) (origin.topRadius() * 0.2),
                origin.topY() - origin.thickness() + 1, 0);
        require(topRadiusDensity > keelRadiusDensity, "keel must recede so the island reads as a flattened inverted pyramid");

        IslandLayout.Island neighbor = firstNeighbor(seed, settings);
        require(neighbor != null, "a neighbouring cell must generate an island");
        int gap = IslandLayout.spacingBetween(origin, neighbor);
        require(gap >= settings.spacingMin() - 8 && gap <= settings.spacingMax() + 8,
                "island centres should sit about 200–300 blocks apart, was " + gap);

        IslandLayout.Island otherSeed = IslandLayout.islandInCell(seed ^ 1L, settings, 3, 2);
        IslandLayout.Island thisSeed = IslandLayout.islandInCell(seed, settings, 3, 2);
        require(otherSeed.centerX() != thisSeed.centerX() || otherSeed.centerZ() != thisSeed.centerZ(),
                "different seeds must move non-origin islands");

        int tileWorldX = WorldDimensions.groupCenter(3, 4);
        int tileWorldZ = WorldDimensions.groupCenter(1, 4);
        IslandLayout.Island landing = IslandLayout.nearestIsland(seed, settings, tileWorldX, tileWorldZ);
        require(Math.abs(IslandLayout.density(seed, settings, 15, origin.topY() - 3, 0)
                        - IslandLayout.density(seed, settings, 16, origin.topY() - 3, 0)) < 0.35,
                "density must be continuous across a chunk seam");
        require(IslandLayout.density(seed, settings, landing.centerX(), landing.topY() - 1, landing.centerZ()) > 0,
                "travel and spawn must land on solid island ground, not a tile-centre void column");
        require(IslandLayout.density(seed, settings, landing.centerX(), landing.topY() + 1, landing.centerZ()) < 0,
                "standing block is one above the flat island top");
        require(IslandLayout.density(seed, settings, origin.centerX(), origin.topY() - 1, origin.centerZ()) > 0,
                "origin spawn must sit on the main island");

        int previousMin = Config.islandSpacingMin;
        int previousMax = Config.islandSpacingMax;
        try {
            Config.islandSpacingMin = 200;
            Config.islandSpacingMax = 300;
            IslandLayout.Settings snap = IslandLayout.Settings.current();
            IslandLayout.Island before = IslandLayout.islandInCell(seed, snap, 4, 0);
            Config.islandSpacingMin = 64;
            Config.islandSpacingMax = 64;
            require(IslandLayout.Settings.current().spacing() != snap.spacing(),
                    "Settings.current() must follow Config");
            IslandLayout.Island afterSnap = IslandLayout.islandInCell(seed, snap, 4, 0);
            require(before.centerX() == afterSnap.centerX() && before.centerZ() == afterSnap.centerZ(),
                    "density compute must keep the settings snapshotted at construction");
            IslandLayout.bindSeed(0x5EEDL);
            require(IslandLayout.worldSeed() == 0x5EEDL,
                    "mapAll rebinds density off IslandLayout.worldSeed, not codec seed 0");
            require(IslandLayout.boundSettings().spacingMin() == 64 && IslandLayout.boundSettings().spacingMax() == 64,
                    "bindSeed must snapshot the spacing mapAll and travel use");
            Config.islandSpacingMin = 512;
            Config.islandSpacingMax = 512;
            require(IslandLayout.boundSettings().spacingMin() == 64,
                    "rebound settings must not track live Config after mapAll");
            IslandLayout.Island live = IslandLayout.islandInCell(seed, IslandLayout.Settings.current(), 4, 0);
            IslandLayout.Island bound = IslandLayout.islandInCell(seed, IslandLayout.boundSettings(), 4, 0);
            require(live.centerX() != bound.centerX() || live.centerZ() != bound.centerZ(),
                    "travel must keep bound spacing instead of live Config");
        } finally {
            Config.islandSpacingMin = previousMin;
            Config.islandSpacingMax = previousMax;
            IslandLayout.bindSeed(0L);
        }
        require(IslandLayout.density(seed, settings, 0, origin.topY() - 2, 0)
                        == IslandLayout.density(seed, settings, 0, origin.topY() - 2, 0),
                "same seed must be deterministic");
        System.out.println("ISLAND_FIELD_TEST_OK originRadius=" + origin.topRadius() + " neighborGap=" + gap);
    }

    private static IslandLayout.Island firstNeighbor(long seed, IslandLayout.Settings settings) {
        return IslandLayout.islandInCell(seed, settings, 1, 0);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
