package net.exmo.exworld.ship.model;

import java.util.Arrays;
import java.util.Collection;

/** Packed local voxel coordinates. Relative axes stay in 0..127 so occupancy fits a 21-bit key. */
public final class ShipOccupancy {
    public static final int MAX_AXIS = 127;
    private static final int MASK = 0x7F;

    private ShipOccupancy() {}

    public static int pack(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x > MAX_AXIS || y > MAX_AXIS || z > MAX_AXIS) {
            throw new IllegalArgumentException("occupancy coordinate out of range: " + x + "," + y + "," + z);
        }
        return (x & MASK) | ((y & MASK) << 7) | ((z & MASK) << 14);
    }

    public static int x(int packed) { return packed & MASK; }
    public static int y(int packed) { return (packed >>> 7) & MASK; }
    public static int z(int packed) { return (packed >>> 14) & MASK; }

    public static int[] sorted(Collection<Integer> positions) {
        int[] packed = positions.stream().mapToInt(Integer::intValue).toArray();
        Arrays.sort(packed);
        return unique(packed);
    }

    public static int[] sorted(int[] positions) {
        int[] copy = Arrays.copyOf(positions, positions.length);
        Arrays.sort(copy);
        return unique(copy);
    }

    public static boolean sameShape(int[] left, int[] right) {
        return Arrays.equals(normalized(left), normalized(right));
    }

    public static int[] normalized(int[] occupancy) {
        if (occupancy.length == 0) return new int[0];
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (int packed : occupancy) {
            minX = Math.min(minX, x(packed));
            minY = Math.min(minY, y(packed));
            minZ = Math.min(minZ, z(packed));
        }
        int[] shifted = new int[occupancy.length];
        for (int i = 0; i < occupancy.length; i++) {
            shifted[i] = pack(x(occupancy[i]) - minX, y(occupancy[i]) - minY, z(occupancy[i]) - minZ);
        }
        Arrays.sort(shifted);
        return shifted;
    }

    public static int minX(int[] occupancy) { return extrema(occupancy, true, 0); }
    public static int minY(int[] occupancy) { return extrema(occupancy, true, 1); }
    public static int minZ(int[] occupancy) { return extrema(occupancy, true, 2); }

    public static int[] translate(int[] occupancy, int dx, int dy, int dz) {
        int[] moved = new int[occupancy.length];
        for (int i = 0; i < occupancy.length; i++) {
            moved[i] = pack(x(occupancy[i]) + dx, y(occupancy[i]) + dy, z(occupancy[i]) + dz);
        }
        Arrays.sort(moved);
        return moved;
    }

    public static boolean contains(int[] occupancy, int packed) {
        return Arrays.binarySearch(occupancy, packed) >= 0;
    }

    private static int extrema(int[] occupancy, boolean minimum, int axis) {
        if (occupancy.length == 0) return 0;
        int value = minimum ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        for (int packed : occupancy) {
            int coordinate = axis == 0 ? x(packed) : axis == 1 ? y(packed) : z(packed);
            value = minimum ? Math.min(value, coordinate) : Math.max(value, coordinate);
        }
        return value;
    }

    private static int[] unique(int[] sorted) {
        int count = 0;
        for (int i = 0; i < sorted.length; i++) {
            if (i == 0 || sorted[i] != sorted[i - 1]) sorted[count++] = sorted[i];
        }
        return count == sorted.length ? sorted : Arrays.copyOf(sorted, count);
    }
}
