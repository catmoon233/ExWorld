package net.exmo.exworld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Client visibility policy: decryption mode and the legacy wall only render the player's chunk group. */
public final class ChunkGroupRenderCuller {
    private ChunkGroupRenderCuller() {}

    public static boolean contains(BlockPos sectionOrigin) {
        return containsSection(sectionOrigin.getX(), sectionOrigin.getZ());
    }

    public static boolean containsSection(int sectionOriginX, int sectionOriginZ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.level.dimension() != Level.OVERWORLD) return true;
        if (!cullOutsideGroup()) return true;
        var shape = groupedShape(minecraft);
        return shape == null || shape.overlapsSection(sectionOriginX, sectionOriginZ);
    }

    /** Uses the same group snapshot for entities that section visibility uses for blocks. */
    public static boolean containsPosition(double worldX, double worldZ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.level.dimension() != Level.OVERWORLD) return true;
        if (!cullOutsideGroup()) return true;
        var shape = groupedShape(minecraft);
        return shape == null || ChunkGroupVisibility.allows(shape, minecraft.player.getX(), minecraft.player.getZ(), worldX, worldZ);
    }

    /** Creative flight still sees past a legacy wall, but decryption mode always hides the outside world. */
    private static boolean cullOutsideGroup() {
        if (!ChunkGroupVisibility.restrictsOutsideGroup()) return false;
        return net.exmo.exworld.Config.decryptionMode || !bypassBoundary();
    }

    /** Creative players ignore a legacy wall. Decryption mode still culls the outside world. */
    public static boolean bypassBoundary() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.isCreative();
    }

    /** Ungrouped cells are not a fallback group: they keep their blocks and draw no wall. */
    private static net.exmo.exworld.world.model.ChunkGroupShape groupedShape(Minecraft minecraft) {
        var shape = ClientChunkGroupState.active();
        if (shape == null || shape.isEmpty() || !shape.containsPosition(minecraft.player.getX(), minecraft.player.getZ())) return null;
        return shape;
    }
}
