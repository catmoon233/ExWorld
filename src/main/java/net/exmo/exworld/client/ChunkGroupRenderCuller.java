package net.exmo.exworld.client;

import net.exmo.exworld.world.model.ChunkGroupBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Client visibility policy: only render sections belonging to the chunk group occupied by the player. */
public final class ChunkGroupRenderCuller {
    private ChunkGroupRenderCuller() {}

    public static boolean contains(BlockPos sectionOrigin) {
        return containsSection(sectionOrigin.getX(), sectionOrigin.getZ());
    }

    public static boolean containsSection(int sectionOriginX, int sectionOriginZ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.level.dimension() != Level.OVERWORLD) return true;
        if (bypassBoundary() || !net.exmo.exworld.Config.legacyRegionBoundary) return true;
        return activeForPlayer(minecraft).overlapsSection(sectionOriginX, sectionOriginZ);
    }

    /** Uses the same group snapshot for entities that section visibility uses for blocks. */
    public static boolean containsPosition(double worldX, double worldZ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.level.dimension() != Level.OVERWORLD) return true;
        if (bypassBoundary() || !net.exmo.exworld.Config.legacyRegionBoundary) return true;
        return ChunkGroupVisibility.allows(activeForPlayer(minecraft), minecraft.player.getX(), minecraft.player.getZ(), worldX, worldZ);
    }

    /** Creative players ignore the active chunk-group boundary: no section culling, no entity hiding, no camera clamp. */
    public static boolean bypassBoundary() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.isCreative();
    }

    private static net.exmo.exworld.world.model.ChunkGroupShape activeForPlayer(Minecraft minecraft) {
        var shape = ClientChunkGroupState.active();
        if (shape != null && shape.containsPosition(minecraft.player.getX(), minecraft.player.getZ())) return shape;
        int groupChunks = shape == null ? net.exmo.exworld.world.model.WorldDimensions.DEFAULT_GROUP_CHUNKS : shape.groupChunks();
        ChunkGroupBounds bounds = ChunkGroupBounds.containing(minecraft.player.getX(), minecraft.player.getZ(), groupChunks);
        return new net.exmo.exworld.world.model.ChunkGroupShape("fallback", groupChunks,
                java.util.List.of(new net.exmo.exworld.world.model.ChunkGroupShape.Cell(bounds.groupX(), bounds.groupZ())));
    }
}
