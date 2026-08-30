package net.exmo.exworld.world.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable connected playable shape assembled from square world-tile cells. */
public final class ChunkGroupShape {
    public static final int NORTH = 1;
    public static final int EAST = 1 << 1;
    public static final int SOUTH = 1 << 2;
    public static final int WEST = 1 << 3;

    private final String id;
    private final int groupChunks;
    private final List<Cell> cells;
    private final List<Cell> boundaryCells;
    private final Set<Long> membership;

    public ChunkGroupShape(String id, int groupChunks, List<Cell> cells) {
        this.id = id;
        this.groupChunks = WorldDimensions.validateGroupChunks(groupChunks);
        this.cells = List.copyOf(cells);
        this.membership = new HashSet<>(cells.size());
        cells.forEach(cell -> membership.add(key(cell.x(), cell.z())));
        this.boundaryCells = this.cells.stream().filter(cell -> boundaryMask(cell) != 0).toList();
    }

    public String id() { return id; }
    public int groupChunks() { return groupChunks; }
    public List<Cell> cells() { return cells; }
    /** Precomputed perimeter cells keep large biome groups cheap to render every frame. */
    public List<Cell> boundaryCells() { return boundaryCells; }
    public boolean isEmpty() { return cells.isEmpty(); }

    public boolean containsGroup(int groupX, int groupZ) {
        return membership.contains(key(groupX, groupZ));
    }

    public boolean containsPosition(double worldX, double worldZ) {
        int groupX = WorldDimensions.groupCoordinate(worldX, groupChunks);
        int groupZ = WorldDimensions.groupCoordinate(worldZ, groupChunks);
        return containsGroup(groupX, groupZ);
    }

    public boolean overlapsSection(int sectionOriginX, int sectionOriginZ) {
        return containsPosition(sectionOriginX + 8.0, sectionOriginZ + 8.0);
    }

    public int boundaryMask(Cell cell) {
        int mask = 0;
        if (!containsGroup(cell.x(), cell.z() - 1)) mask |= NORTH;
        if (!containsGroup(cell.x() + 1, cell.z())) mask |= EAST;
        if (!containsGroup(cell.x(), cell.z() + 1)) mask |= SOUTH;
        if (!containsGroup(cell.x() - 1, cell.z())) mask |= WEST;
        return mask;
    }

    private static long key(int x, int z) { return (long) x << 32 ^ z & 0xFFFFFFFFL; }

    public record Cell(int x, int z) {}
}
