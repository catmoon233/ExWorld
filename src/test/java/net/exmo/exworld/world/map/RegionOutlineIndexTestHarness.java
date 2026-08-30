package net.exmo.exworld.world.map;

import net.exmo.exworld.world.model.MapTile;

import java.util.List;

/** Regression harness: a 2x2 merged region has one perimeter and no internal square seams. */
public final class RegionOutlineIndexTestHarness {
    public static void main(String[] args) {
        MapTile northWest = tile("nw", 0, 0);
        MapTile northEast = tile("ne", 1, 0);
        MapTile southWest = tile("sw", 0, 1);
        MapTile southEast = tile("se", 1, 1);
        RegionOutlineIndex outlines = new RegionOutlineIndex(List.of(northWest, northEast, southWest, southEast));

        require(outlines.mask(northWest) == (RegionOutlineIndex.NORTH | RegionOutlineIndex.WEST),
                "north-west internal edges leaked");
        require(outlines.mask(northEast) == (RegionOutlineIndex.NORTH | RegionOutlineIndex.EAST),
                "north-east internal edges leaked");
        require(outlines.mask(southWest) == (RegionOutlineIndex.SOUTH | RegionOutlineIndex.WEST),
                "south-west internal edges leaked");
        require(outlines.mask(southEast) == (RegionOutlineIndex.SOUTH | RegionOutlineIndex.EAST),
                "south-east internal edges leaked");
        System.out.println("REGION_OUTLINE_TEST_OK");
    }

    private static MapTile tile(String id, int x, int z) {
        return new MapTile(id, x, z, "merged", "prairie");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
