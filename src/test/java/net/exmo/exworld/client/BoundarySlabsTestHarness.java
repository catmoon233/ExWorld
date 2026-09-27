package net.exmo.exworld.client;

import net.exmo.exworld.world.model.ChunkGroupShape;

import java.util.List;

/** Checks that region barriers join into one slab and disappear outside the render window. */
public final class BoundarySlabsTestHarness {
    public static void main(String[] args) {
        ChunkGroupShape row = new ChunkGroupShape("row", 1, List.of(
                new ChunkGroupShape.Cell(0, 0), new ChunkGroupShape.Cell(1, 0)));
        List<BoundarySlabs.Slab> open = BoundarySlabs.visible(row, 0, 5, 0.28, -64, -64, 64, 64);
        require(countNorth(open, 0) == 1, "adjacent north edges must be one slab, not a line at the join");
        BoundarySlabs.Slab north = northAt(open, 0);
        require(north.minX() == 0 && north.maxX() == 32, "merged north slab must span both cells");

        List<BoundarySlabs.Slab> clipped = BoundarySlabs.visible(row, 0, 5, 0.28, 8, -16, 24, 16);
        BoundarySlabs.Slab clippedNorth = northAt(clipped, 0);
        require(clippedNorth.minX() == 8 && clippedNorth.maxX() == 24, "slab must be cut to the render window");
        require(BoundarySlabs.visible(row, 0, 5, 0.28, -64, 64, 64, 128).isEmpty(),
                "a wall beyond the render window must not be drawn");

        ChunkGroupShape box = new ChunkGroupShape("box", 1, List.of(new ChunkGroupShape.Cell(0, 0)));
        List<BoundarySlabs.Slab> walls = BoundarySlabs.visible(box, 0, 5, 0.28, -64, -64, 64, 64);
        require(walls.size() == 4, "a single cell still has four outer walls");
        require(!overlaps(walls), "convex corners must not overlap, or their shared faces draw lines");

        ChunkGroupShape elbow = new ChunkGroupShape("elbow", 1, List.of(
                new ChunkGroupShape.Cell(0, 0), new ChunkGroupShape.Cell(1, 0), new ChunkGroupShape.Cell(0, 1)));
        List<BoundarySlabs.Slab> concave = BoundarySlabs.visible(elbow, 0, 5, 0.28, -64, -64, 64, 64);
        require(!overlaps(concave), "concave corners must stay closed without overlapping volumes");
        List<BoundarySlabs.Slab> caps = BoundarySlabs.caps(row, 5, 0.28, -64, -64, 64, 64);
        require(caps.size() == 1, "adjacent cells must share one lid, not a seam");
        require(caps.getFirst().minX() == 0 && caps.getFirst().maxX() == 32, "lid must cover both cells");
        require(Math.abs(caps.getFirst().minY() - 4.72) < 1.0E-9 && caps.getFirst().maxY() == 5, "lid must sit on the reduced top");
        require(BoundarySlabs.caps(row, 5, 0.28, 8, -16, 24, 16).getFirst().minX() == 8, "lid must stop at the render window");
        require(BoundarySlabs.caps(row, 5, 0.28, -64, 64, 64, 128).isEmpty(), "a lid beyond the render window must not be drawn");
        List<BoundarySlabs.Slab> elbowCaps = BoundarySlabs.caps(elbow, 5, 0.28, -64, -64, 64, 64);
        require(elbowCaps.size() == 2, "a concave region lid splits only where the outline turns");
        require(!overlaps(elbowCaps), "concave lid pieces must not share a face");
        System.out.println("BOUNDARY_SLABS_TEST_OK");
    }

    private static int countNorth(List<BoundarySlabs.Slab> slabs, double z) {
        int count = 0;
        for (BoundarySlabs.Slab slab : slabs) if (slab.minZ() == z) count++;
        return count;
    }

    private static BoundarySlabs.Slab northAt(List<BoundarySlabs.Slab> slabs, double z) {
        for (BoundarySlabs.Slab slab : slabs) if (slab.minZ() == z) return slab;
        throw new AssertionError("missing north slab at " + z);
    }

    private static boolean overlaps(List<BoundarySlabs.Slab> slabs) {
        for (int i = 0; i < slabs.size(); i++) {
            for (int j = i + 1; j < slabs.size(); j++) {
                BoundarySlabs.Slab a = slabs.get(i);
                BoundarySlabs.Slab b = slabs.get(j);
                if (a.minX() < b.maxX() && b.minX() < a.maxX() && a.minZ() < b.maxZ() && b.minZ() < a.maxZ()) return true;
            }
        }
        return false;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
