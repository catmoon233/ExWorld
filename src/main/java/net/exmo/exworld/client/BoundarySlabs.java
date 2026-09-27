package net.exmo.exworld.client;

import net.exmo.exworld.world.model.ChunkGroupBounds;
import net.exmo.exworld.world.model.ChunkGroupShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Turns a chunk-group perimeter into continuous slabs.
 * Adjacent cells used to be separate boxes, so their shared end faces showed up as lines.
 */
public final class BoundarySlabs {
    /** Keeps a convex corner's end cap off the crossing slab so the two faces do not z-fight. */
    private static final double CORNER_GAP = 0.01;

    private BoundarySlabs() {}

    public record Slab(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {}

    public static List<Slab> visible(ChunkGroupShape shape, double bottom, double top, double thickness,
                                     double windowMinX, double windowMinZ, double windowMaxX, double windowMaxZ) {
        List<Run> runs = merge(shape);
        insetConvexEnds(runs, thickness);
        List<Slab> slabs = new ArrayList<>();
        for (Run run : runs) {
            if (run.start >= run.end) continue;
            Slab clipped = clip(run.toSlab(bottom, top, thickness), windowMinX, windowMinZ, windowMaxX, windowMaxZ);
            if (clipped != null) slabs.add(clipped);
        }
        return slabs;
    }

    /** One thin sheet over the playable cells, merged so neighbouring cells do not draw a seam. */
    public static List<Slab> caps(ChunkGroupShape shape, double top, double thickness,
                                  double windowMinX, double windowMinZ, double windowMaxX, double windowMaxZ) {
        List<Footprint> footprints = mergeFootprints(shape);
        separateSharedEdges(footprints);
        double bottom = top - thickness;
        List<Slab> slabs = new ArrayList<>();
        for (Footprint footprint : footprints) {
            if (footprint.minX >= footprint.maxX || footprint.minZ >= footprint.maxZ) continue;
            Slab clipped = clip(new Slab(footprint.minX, bottom, footprint.minZ, footprint.maxX, top, footprint.maxZ),
                    windowMinX, windowMinZ, windowMaxX, windowMaxZ);
            if (clipped != null) slabs.add(clipped);
        }
        return slabs;
    }

    private static List<Run> merge(ChunkGroupShape shape) {
        Map<Key, List<Run>> grouped = new LinkedHashMap<>();
        for (ChunkGroupShape.Cell cell : shape.boundaryCells()) {
            ChunkGroupBounds bounds = ChunkGroupBounds.forGroup(cell.x(), cell.z(), shape.groupChunks());
            int edges = shape.boundaryMask(cell);
            if ((edges & ChunkGroupShape.NORTH) != 0) add(grouped, Run.alongX(bounds.minZ(), 1, bounds.minX(), bounds.maxX()));
            if ((edges & ChunkGroupShape.SOUTH) != 0) add(grouped, Run.alongX(bounds.maxZ(), -1, bounds.minX(), bounds.maxX()));
            if ((edges & ChunkGroupShape.WEST) != 0) add(grouped, Run.alongZ(bounds.minX(), 1, bounds.minZ(), bounds.maxZ()));
            if ((edges & ChunkGroupShape.EAST) != 0) add(grouped, Run.alongZ(bounds.maxX(), -1, bounds.minZ(), bounds.maxZ()));
        }
        List<Run> merged = new ArrayList<>();
        for (List<Run> runs : grouped.values()) {
            runs.sort(Comparator.comparingDouble(run -> run.start));
            Run current = runs.getFirst();
            for (int i = 1; i < runs.size(); i++) {
                Run next = runs.get(i);
                if (next.start <= current.end + 1.0E-4) {
                    current.end = Math.max(current.end, next.end);
                } else {
                    merged.add(current);
                    current = next;
                }
            }
            merged.add(current);
        }
        return merged;
    }

    /** East/west runs stop where a north/south slab already covers the convex corner, so the volumes do not overlap. */
    private static void insetConvexEnds(List<Run> runs, double thickness) {
        List<Run> horizontal = runs.stream().filter(run -> run.alongX).toList();
        for (Run wall : runs) {
            if (wall.alongX) continue;
            double minX = Math.min(wall.edge, wall.edge + wall.inward * thickness);
            double maxX = Math.max(wall.edge, wall.edge + wall.inward * thickness);
            if (covers(horizontal, wall.start, 1, minX, maxX)) wall.start += thickness + CORNER_GAP;
            if (covers(horizontal, wall.end, -1, minX, maxX)) wall.end -= thickness + CORNER_GAP;
        }
    }

    private static boolean covers(List<Run> horizontal, double edge, int inward, double minX, double maxX) {
        for (Run run : horizontal) {
            if (run.inward == inward && run.edge == edge && run.start < maxX && run.end > minX) return true;
        }
        return false;
    }

    private static Slab clip(Slab slab, double minX, double minZ, double maxX, double maxZ) {
        double x0 = Math.max(slab.minX(), minX);
        double x1 = Math.min(slab.maxX(), maxX);
        double z0 = Math.max(slab.minZ(), minZ);
        double z1 = Math.min(slab.maxZ(), maxZ);
        if (x0 >= x1 || z0 >= z1) return null;
        return new Slab(x0, slab.minY(), z0, x1, slab.maxY(), z1);
    }

    private static void add(Map<Key, List<Run>> grouped, Run run) {
        grouped.computeIfAbsent(run.key(), ignored -> new ArrayList<>()).add(run);
    }

    private static List<Footprint> mergeFootprints(ChunkGroupShape shape) {
        Map<Integer, List<Integer>> columns = new TreeMap<>();
        for (ChunkGroupShape.Cell cell : shape.cells()) {
            columns.computeIfAbsent(cell.z(), ignored -> new ArrayList<>()).add(cell.x());
        }
        List<Span> spans = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : columns.entrySet()) {
            List<Integer> xs = entry.getValue();
            xs.sort(Integer::compareTo);
            int start = xs.getFirst();
            int previous = start;
            for (int i = 1; i < xs.size(); i++) {
                int x = xs.get(i);
                if (x == previous + 1) {
                    previous = x;
                } else {
                    spans.add(new Span(entry.getKey(), start, previous + 1));
                    start = previous = x;
                }
            }
            spans.add(new Span(entry.getKey(), start, previous + 1));
        }
        boolean[] used = new boolean[spans.size()];
        List<Footprint> footprints = new ArrayList<>();
        for (int i = 0; i < spans.size(); i++) {
            if (used[i]) continue;
            Span span = spans.get(i);
            int zEnd = span.z + 1;
            used[i] = true;
            boolean extended = true;
            while (extended) {
                extended = false;
                for (int j = 0; j < spans.size(); j++) {
                    if (used[j]) continue;
                    Span next = spans.get(j);
                    if (next.z == zEnd && next.x0 == span.x0 && next.x1 == span.x1) {
                        used[j] = true;
                        zEnd++;
                        extended = true;
                        break;
                    }
                }
            }
            ChunkGroupBounds origin = ChunkGroupBounds.forGroup(span.x0, span.z, shape.groupChunks());
            ChunkGroupBounds far = ChunkGroupBounds.forGroup(span.x1 - 1, zEnd - 1, shape.groupChunks());
            footprints.add(new Footprint(origin.minX(), origin.minZ(), far.maxX(), far.maxZ()));
        }
        return footprints;
    }

    /** A concave join is two rectangles sharing a face. Pull one back so that face is not drawn twice. */
    private static void separateSharedEdges(List<Footprint> footprints) {
        for (int i = 0; i < footprints.size(); i++) {
            for (int j = i + 1; j < footprints.size(); j++) {
                Footprint a = footprints.get(i);
                Footprint b = footprints.get(j);
                if (overlaps(a.minX, a.maxX, b.minX, b.maxX)) {
                    if (Math.abs(a.maxZ - b.minZ) < 1.0E-4) b.minZ += CORNER_GAP;
                    else if (Math.abs(b.maxZ - a.minZ) < 1.0E-4) a.minZ += CORNER_GAP;
                }
                if (overlaps(a.minZ, a.maxZ, b.minZ, b.maxZ)) {
                    if (Math.abs(a.maxX - b.minX) < 1.0E-4) b.minX += CORNER_GAP;
                    else if (Math.abs(b.maxX - a.minX) < 1.0E-4) a.minX += CORNER_GAP;
                }
            }
        }
    }

    private static boolean overlaps(double minA, double maxA, double minB, double maxB) {
        return minA < maxB && minB < maxA;
    }

    private record Span(int z, int x0, int x1) {}

    private static final class Footprint {
        private double minX;
        private double minZ;
        private double maxX;
        private double maxZ;

        private Footprint(double minX, double minZ, double maxX, double maxZ) {
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
        }
    }

    private static final class Run {
        private final boolean alongX;
        private final double edge;
        private final int inward;
        private double start;
        private double end;

        private Run(boolean alongX, double edge, int inward, double start, double end) {
            this.alongX = alongX;
            this.edge = edge;
            this.inward = inward;
            this.start = start;
            this.end = end;
        }

        private static Run alongX(double edge, int inward, double start, double end) {
            return new Run(true, edge, inward, start, end);
        }

        private static Run alongZ(double edge, int inward, double start, double end) {
            return new Run(false, edge, inward, start, end);
        }

        private Key key() {
            return new Key(alongX, Double.doubleToLongBits(edge), inward);
        }

        private Slab toSlab(double bottom, double top, double thickness) {
            double near = edge;
            double far = edge + inward * thickness;
            double min = Math.min(near, far);
            double max = Math.max(near, far);
            return alongX
                    ? new Slab(start, bottom, min, end, top, max)
                    : new Slab(min, bottom, start, max, top, end);
        }
    }

    private record Key(boolean alongX, long edgeBits, int inward) {}
}
