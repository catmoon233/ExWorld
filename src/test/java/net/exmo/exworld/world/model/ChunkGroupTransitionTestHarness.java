package net.exmo.exworld.world.model;

/** Regression checks for overworld re-entry synchronization and group-only subtitles. */
public final class ChunkGroupTransitionTestHarness {
    public static void main(String[] args) {
        require(ChunkGroupTransition.shouldSynchronize(true, "forest", "forest"),
                "returning from a dungeon must reinstall the active group even when the group did not change");
        require(ChunkGroupTransition.shouldSynchronize(false, "forest", "ruins"),
                "crossing into another group must synchronize its shape");
        require(!ChunkGroupTransition.shouldSynchronize(false, "forest", "forest"),
                "moving inside a group must not rebuild client visibility");
        require(ChunkGroupTransition.shouldShowSubtitle("forest", "ruins"),
                "entering another group must announce it");
        require(!ChunkGroupTransition.shouldShowSubtitle("forest", "forest"),
                "crossing world tiles inside one group must not repeat its subtitle");
        System.out.println("CHUNK_GROUP_TRANSITION_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
