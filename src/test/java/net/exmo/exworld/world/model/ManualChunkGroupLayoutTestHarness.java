package net.exmo.exworld.world.model;

import java.util.List;

/** Regression checks for manual group coverage, connectivity, icon metadata, and singleton fallback. */
public final class ManualChunkGroupLayoutTestHarness {
    public static void main(String[] args) {
        List<WorldTile> tiles = List.of(tile("a", 0, 0), tile("b", 1, 0), tile("c", 0, 1), tile("d", 1, 1));
        List<ManualChunkGroupLayout.Group> singletons = ManualChunkGroupLayout.singletons(tiles);
        require(singletons.size() == 4 && singletons.stream().allMatch(group -> group.tileIds().size() == 1),
                "unassigned world tiles must fall back to individual groups");

        ManualChunkGroupLayout.Applied applied = ManualChunkGroupLayout.apply(tiles, List.of(
                new ManualChunkGroupLayout.Group("north", "北境", "*", "北境哨站", "铁矿、松木", true, List.of("a", "b", "c")),
                new ManualChunkGroupLayout.Group("south", "南境", "", List.of("d"))));
        require(applied.tiles().stream().filter(tile -> tile.id().equals("b")).findFirst().orElseThrow().regionId().equals("north"),
                "manual membership must rewrite a tile's active group");
        Region north = applied.regions().stream().filter(region -> region.id().equals("north")).findFirst().orElseThrow();
        require(north.icon().equals("*") && north.configured() && north.site().equals("北境哨站")
                        && north.resources().equals("铁矿、松木"),
                "group metadata must persist with the group instead of a single tile");

        requireThrows(() -> ManualChunkGroupLayout.apply(tiles, List.of(
                new ManualChunkGroupLayout.Group("split", "断开", "", List.of("a", "d")),
                new ManualChunkGroupLayout.Group("rest", "其余", "", List.of("b", "c")))),
                "disconnected manual groups must be rejected");
        requireThrows(() -> ManualChunkGroupLayout.apply(tiles, List.of(
                new ManualChunkGroupLayout.Group("partial", "不完整", "", List.of("a", "b", "c")))),
                "every world tile must belong to exactly one saved group");
        System.out.println("MANUAL_CHUNK_GROUP_LAYOUT_TEST_OK");
    }

    private static WorldTile tile(String id, int x, int z) {
        return new WorldTile(id, x, z, "old", id, 0, x * 64, z * 64, false,
                WorldBiome.PRAIRIE.id(), "", "", "");
    }

    private static void requireThrows(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
