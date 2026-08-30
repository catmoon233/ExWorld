package net.exmo.exworld.world.model;

import java.util.Objects;

/** Small transition seam shared by server movement and regression checks. */
public final class ChunkGroupTransition {
    private ChunkGroupTransition() {}

    public static boolean shouldSynchronize(boolean enteredOverworld, String previousGroupId, String currentGroupId) {
        return enteredOverworld || !Objects.equals(previousGroupId, currentGroupId);
    }

    public static boolean shouldShowSubtitle(String previousGroupId, String currentGroupId) {
        return !Objects.equals(previousGroupId, currentGroupId);
    }
}
