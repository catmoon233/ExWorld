package net.exmo.exworld.ship.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Compact 船体结构: palette of block keys plus packed local coordinates.
 * Containers only store non-empty slots. The hull is immutable; edits return a new instance and bump revision.
 */
public final class ShipHull {
    public static final int MAX_BLOCKS = 4096;
    public static final int MAX_AXIS = ShipOccupancy.MAX_AXIS;

    private final List<String> palette;
    private final int[] positions;
    private final short[] states;
    private final Map<Integer, List<ShipSlot>> containers;
    private final int revision;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    private ShipHull(List<String> palette, int[] positions, short[] states, Map<Integer, List<ShipSlot>> containers,
                     int revision, int sizeX, int sizeY, int sizeZ) {
        this.palette = palette;
        this.positions = positions;
        this.states = states;
        this.containers = containers;
        this.revision = revision;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public static ShipHull empty() {
        return new ShipHull(List.of(), new int[0], new short[0], Map.of(), 0, 0, 0, 0);
    }

    public static ShipHull of(List<ShipBlock> blocks) { return of(blocks, 0); }

    public static ShipHull of(List<ShipBlock> blocks, int revision) {
        if (blocks == null || blocks.isEmpty()) return new ShipHull(List.of(), new int[0], new short[0], Map.of(), revision, 0, 0, 0);
        if (blocks.size() > MAX_BLOCKS) throw new IllegalArgumentException("hull exceeds " + MAX_BLOCKS + " blocks");
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (ShipBlock block : blocks) {
            minX = Math.min(minX, block.x());
            minY = Math.min(minY, block.y());
            minZ = Math.min(minZ, block.z());
        }
        LinkedHashMap<Integer, ShipBlock> unique = new LinkedHashMap<>();
        for (ShipBlock block : blocks) {
            int x = block.x() - minX, y = block.y() - minY, z = block.z() - minZ;
            if (x > MAX_AXIS || y > MAX_AXIS || z > MAX_AXIS) {
                throw new IllegalArgumentException("hull spans more than " + (MAX_AXIS + 1) + " blocks on an axis");
            }
            unique.put(ShipOccupancy.pack(x, y, z), new ShipBlock(x, y, z, block.block(), block.slots()));
        }
        return assemble(new ArrayList<>(unique.values()), revision);
    }

    public static Builder builder() { return new Builder(); }

    public List<String> palette() { return palette; }
    public int revision() { return revision; }
    public int sizeX() { return sizeX; }
    public int sizeY() { return sizeY; }
    public int sizeZ() { return sizeZ; }
    public int width() { return sizeX; }
    public int height() { return sizeY; }
    public int depth() { return sizeZ; }
    public int size() { return positions.length; }
    public boolean isEmpty() { return positions.length == 0; }
    public int[] occupancy() { return positions.clone(); }
    public Map<Integer, List<ShipSlot>> containers() { return containers; }

    public Optional<String> blockAt(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) return Optional.empty();
        int index = indexOf(ShipOccupancy.pack(x, y, z));
        if (index < 0) return Optional.empty();
        return Optional.of(palette.get(states[index]));
    }

    public Optional<String> blockAtPacked(int packed) {
        int index = indexOf(packed);
        return index < 0 ? Optional.empty() : Optional.of(palette.get(states[index]));
    }

    public boolean occupied(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) return false;
        return indexOf(ShipOccupancy.pack(x, y, z)) >= 0;
    }

    public boolean occupiedPacked(int packed) { return indexOf(packed) >= 0; }

    public List<ShipSlot> containerAt(int packed) { return containers.getOrDefault(packed, List.of()); }

    public List<ShipBlock> blocks() {
        List<ShipBlock> blocks = new ArrayList<>(positions.length);
        for (int i = 0; i < positions.length; i++) {
            int packed = positions[i];
            blocks.add(new ShipBlock(ShipOccupancy.x(packed), ShipOccupancy.y(packed), ShipOccupancy.z(packed),
                    palette.get(states[i]), containers.getOrDefault(packed, List.of())));
        }
        return blocks;
    }

    public ShipHull withRevision(int next) {
        return new ShipHull(palette, positions, states, containers, next, sizeX, sizeY, sizeZ);
    }

    public ShipHull bumped() { return withRevision(revision + 1); }

    public ShipHull setBlock(int x, int y, int z, String block, List<ShipSlot> slots) {
        List<ShipBlock> blocks = blocks();
        int packed = ShipOccupancy.pack(x, y, z);
        boolean replaced = false;
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).packed() == packed) {
                blocks.set(i, new ShipBlock(x, y, z, block, slots));
                replaced = true;
                break;
            }
        }
        if (!replaced) blocks.add(new ShipBlock(x, y, z, block, slots));
        return of(blocks, revision + 1);
    }

    public ShipHull setBlockKey(int x, int y, int z, String block) {
        int packed = ShipOccupancy.pack(x, y, z);
        return setBlock(x, y, z, block, containerAt(packed));
    }

    public ShipHull withContainer(int packed, List<ShipSlot> slots) {
        List<ShipBlock> blocks = blocks();
        for (int i = 0; i < blocks.size(); i++) {
            ShipBlock block = blocks.get(i);
            if (block.packed() == packed) {
                blocks.set(i, new ShipBlock(block.x(), block.y(), block.z(), block.block(), slots));
                break;
            }
        }
        return of(blocks, revision);
    }

    /** Replace occupancy voxels with variant voxels after aligning both shapes to their own minimum corner. */
    public ShipHull replaceOccupancy(int[] occupancy, ShipHull variant) {
        if (occupancy.length == 0) throw new IllegalArgumentException("occupancy is empty");
        int[] part = ShipOccupancy.sorted(occupancy);
        if (!ShipOccupancy.sameShape(part, variant.occupancy())) {
            throw new IllegalArgumentException("variant occupancy does not match the part mask");
        }
        int dx = ShipOccupancy.minX(part) - ShipOccupancy.minX(variant.occupancy());
        int dy = ShipOccupancy.minY(part) - ShipOccupancy.minY(variant.occupancy());
        int dz = ShipOccupancy.minZ(part) - ShipOccupancy.minZ(variant.occupancy());
        LinkedHashMap<Integer, ShipBlock> next = new LinkedHashMap<>();
        for (ShipBlock block : blocks()) {
            if (!ShipOccupancy.contains(part, block.packed())) next.put(block.packed(), block);
        }
        for (ShipBlock block : variant.blocks()) {
            int x = block.x() + dx, y = block.y() + dy, z = block.z() + dz;
            ShipBlock placed = new ShipBlock(x, y, z, block.block(), block.slots());
            next.put(placed.packed(), placed);
        }
        return of(new ArrayList<>(next.values()), revision + 1);
    }

    public double boundsVolume() { return (double) sizeX * sizeY * sizeZ; }

    private int indexOf(int packed) { return Arrays.binarySearch(positions, packed); }

    private static ShipHull assemble(List<ShipBlock> originBlocks, int revision) {
        originBlocks.sort((left, right) -> Integer.compare(left.packed(), right.packed()));
        List<String> palette = new ArrayList<>();
        Map<String, Integer> indexByKey = new LinkedHashMap<>();
        int[] positions = new int[originBlocks.size()];
        short[] states = new short[originBlocks.size()];
        Map<Integer, List<ShipSlot>> containers = new LinkedHashMap<>();
        int maxX = 0, maxY = 0, maxZ = 0;
        for (int i = 0; i < originBlocks.size(); i++) {
            ShipBlock block = originBlocks.get(i);
            int paletteIndex = indexByKey.computeIfAbsent(block.block(), key -> {
                palette.add(key);
                return palette.size() - 1;
            });
            if (paletteIndex > Short.MAX_VALUE) throw new IllegalArgumentException("palette is too large");
            positions[i] = block.packed();
            states[i] = (short) paletteIndex;
            if (block.hasContainer()) containers.put(block.packed(), block.slots());
            maxX = Math.max(maxX, block.x() + 1);
            maxY = Math.max(maxY, block.y() + 1);
            maxZ = Math.max(maxZ, block.z() + 1);
        }
        return new ShipHull(List.copyOf(palette), positions, states, Collections.unmodifiableMap(containers),
                revision, maxX, maxY, maxZ);
    }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ShipHull hull)) return false;
        return revision == hull.revision && palette.equals(hull.palette) && Arrays.equals(positions, hull.positions)
                && Arrays.equals(states, hull.states) && containers.equals(hull.containers);
    }

    @Override public int hashCode() {
        return Objects.hash(palette, Arrays.hashCode(positions), Arrays.hashCode(states), containers, revision);
    }

    public static final class Builder {
        private final List<ShipBlock> blocks = new ArrayList<>();

        public Builder add(int x, int y, int z, String block) { return add(x, y, z, block, List.of()); }

        public Builder add(int x, int y, int z, String block, List<ShipSlot> slots) {
            blocks.add(new ShipBlock(x, y, z, block, slots));
            return this;
        }

        public Builder add(ShipBlock block) { blocks.add(block); return this; }

        public ShipHull build() { return ShipHull.of(blocks, 0); }
        public ShipHull build(int revision) { return ShipHull.of(blocks, revision); }
    }
}
