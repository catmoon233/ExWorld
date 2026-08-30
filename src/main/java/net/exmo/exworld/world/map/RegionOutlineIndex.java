package net.exmo.exworld.world.map;

import net.exmo.exworld.world.model.MapTile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Precomputed outer-edge masks for merged, possibly irregular regions. */
public final class RegionOutlineIndex {
    public static final int NORTH = 1;
    public static final int EAST = 1 << 1;
    public static final int SOUTH = 1 << 2;
    public static final int WEST = 1 << 3;

    private final Map<String, Integer> masks;

    public RegionOutlineIndex(List<MapTile> tiles) {
        Map<Long, MapTile> grid = new HashMap<>();
        tiles.forEach(tile -> grid.put(key(tile.mapX(), tile.mapZ()), tile));
        masks = new HashMap<>(tiles.size());
        for (MapTile tile : tiles) {
            int mask = 0;
            if (!sameRegion(grid, tile, 0, -1)) mask |= NORTH;
            if (!sameRegion(grid, tile, 1, 0)) mask |= EAST;
            if (!sameRegion(grid, tile, 0, 1)) mask |= SOUTH;
            if (!sameRegion(grid, tile, -1, 0)) mask |= WEST;
            masks.put(tile.id(), mask);
        }
    }

    public int mask(MapTile tile) {
        return masks.getOrDefault(tile.id(), NORTH | EAST | SOUTH | WEST);
    }

    private static boolean sameRegion(Map<Long, MapTile> grid, MapTile tile, int dx, int dz) {
        MapTile neighbor = grid.get(key(tile.mapX() + dx, tile.mapZ() + dz));
        return neighbor != null && neighbor.regionId().equals(tile.regionId());
    }

    private static long key(int x, int z) {
        return (long) x << 32 ^ z & 0xFFFFFFFFL;
    }
}
