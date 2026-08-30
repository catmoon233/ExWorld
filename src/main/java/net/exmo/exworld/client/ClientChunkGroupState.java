package net.exmo.exworld.client;

import net.exmo.exworld.world.model.ChunkGroupShape;
import net.minecraft.client.Minecraft;

/** Client-owned active group snapshot read safely by vanilla and Sodium visibility hooks. */
public final class ClientChunkGroupState {
    private static volatile ChunkGroupShape active;
    private static volatile long revision;

    private ClientChunkGroupState() {}

    public static void install(ChunkGroupShape shape) {
        active = shape;
        revision++;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) minecraft.levelRenderer.allChanged();
    }

    public static ChunkGroupShape active() { return active; }
    public static long revision() { return revision; }
}
