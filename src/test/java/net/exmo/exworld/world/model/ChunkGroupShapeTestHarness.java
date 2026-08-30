package net.exmo.exworld.world.model;

import java.util.List;

/** Regression harness for concave group membership and merged outer edges. */
public final class ChunkGroupShapeTestHarness {
    public static void main(String[] args) {
        ChunkGroupShape shape = new ChunkGroupShape("l_shape", 4, List.of(
                new ChunkGroupShape.Cell(0, 0), new ChunkGroupShape.Cell(1, 0), new ChunkGroupShape.Cell(0, 1)));
        require(shape.containsPosition(0, 0), "origin must be playable");
        require(shape.containsPosition(64, 0), "east arm must be playable");
        require(shape.containsPosition(0, 64), "south arm must be playable");
        require(!shape.containsPosition(64, 64), "concave corner must remain outside");
        require((shape.boundaryMask(new ChunkGroupShape.Cell(0, 0)) & ChunkGroupShape.EAST) == 0,
                "shared east edge must be merged");
        require((shape.boundaryMask(new ChunkGroupShape.Cell(0, 0)) & ChunkGroupShape.SOUTH) == 0,
                "shared south edge must be merged");
        require(shape.boundaryCells().size() == 3, "each cell of the L shape touches its perimeter");
        System.out.println("CHUNK_GROUP_SHAPE_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
