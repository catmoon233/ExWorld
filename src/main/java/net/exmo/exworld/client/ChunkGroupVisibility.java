package net.exmo.exworld.client;

import net.exmo.exworld.world.model.ChunkGroupShape;

/** Pure entity-visibility rule, kept independent of Minecraft's renderer lifecycle. */
public final class ChunkGroupVisibility {
    private ChunkGroupVisibility() {}

    public static boolean allows(ChunkGroupShape active, double playerX, double playerZ,
                                 double candidateX, double candidateZ) {
        if (!restrictsOutsideGroup()) return true;
        return active == null || active.containsPosition(candidateX, candidateZ);
    }

    /** Legacy walls and decryption mode both hide the world outside the active group. */
    public static boolean restrictsOutsideGroup() {
        return net.exmo.exworld.Config.legacyRegionBoundary || net.exmo.exworld.Config.decryptionMode;
    }
}
