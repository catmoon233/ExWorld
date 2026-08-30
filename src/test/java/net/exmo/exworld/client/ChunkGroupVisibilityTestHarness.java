package net.exmo.exworld.client;

import net.exmo.exworld.world.model.ChunkGroupShape;

import java.util.List;

/** Regression checks for world entities outside the active irregular group. */
public final class ChunkGroupVisibilityTestHarness {
    public static void main(String[] args) {
        ChunkGroupShape shape = new ChunkGroupShape("l", 4, List.of(
                new ChunkGroupShape.Cell(0, 0), new ChunkGroupShape.Cell(1, 0), new ChunkGroupShape.Cell(0, 1)));
        require(ChunkGroupVisibility.allows(shape, 0, 0, 64, 0), "entity inside the active group must render");
        require(!ChunkGroupVisibility.allows(shape, 0, 0, 64, 64),
                "entity outside the active concave group must not render");
        require(ChunkGroupVisibility.allows(null, 0, 0, 9_999, 9_999),
                "without an authoritative group snapshot, normal entity rendering remains available");
        System.out.println("CHUNK_GROUP_VISIBILITY_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
