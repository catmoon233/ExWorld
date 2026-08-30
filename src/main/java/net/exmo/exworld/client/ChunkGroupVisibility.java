package net.exmo.exworld.client;

import net.exmo.exworld.world.model.ChunkGroupShape;

/** Pure entity-visibility rule, kept independent of Minecraft's renderer lifecycle. */
public final class ChunkGroupVisibility {
    private ChunkGroupVisibility() {}

    public static boolean allows(ChunkGroupShape active, double playerX, double playerZ,
                                 double candidateX, double candidateZ) {
        return active == null || active.containsPosition(candidateX, candidateZ);
    }
}
