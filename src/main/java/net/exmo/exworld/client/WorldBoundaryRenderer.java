package net.exmo.exworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.exmo.exworld.world.generation.IslandLayout;
import net.exmo.exworld.world.model.ChunkGroupBounds;
import net.exmo.exworld.Config;
import net.exmo.exworld.client.battle.BattleClient;
import net.exmo.exworld.world.model.ChunkGroupShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Renders the outer edge of the active chunk group, never individual Minecraft chunk edges. */
public final class WorldBoundaryRenderer {
    private WorldBoundaryRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (BattleClient.active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.level.dimension() != Level.OVERWORLD) return;
        if (ChunkGroupRenderCuller.bypassBoundary()) return;
        ChunkGroupShape shape = ClientChunkGroupState.active();
        if (shape == null || !shape.containsPosition(minecraft.player.getX(), minecraft.player.getZ())) {
            int groupChunks = net.exmo.exworld.world.model.WorldDimensions.DEFAULT_GROUP_CHUNKS;
            ChunkGroupBounds bounds = ChunkGroupBounds.containing(minecraft.player.getX(), minecraft.player.getZ(), groupChunks);
            shape = new ChunkGroupShape("fallback", groupChunks, java.util.List.of(
                    new ChunkGroupShape.Cell(bounds.groupX(), bounds.groupZ())));
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        boolean legacy = Config.legacyRegionBoundary;
        double bottom;
        double top;
        if (legacy) {
            bottom = minecraft.level.getMinBuildHeight();
            top = minecraft.level.getMaxBuildHeight();
        } else if (ClientChunkGroupState.archipelago()) {
            // A fixed 5-block band at the island-top level, never following the player up and down.
            bottom = IslandLayout.DEFAULT_MIN_Y;
            top = IslandLayout.DEFAULT_MIN_Y + 5.0;
        } else {
            bottom = minecraft.player.getY() - 2.0;
            top = minecraft.player.getY() + 3.0;
        }
        double thickness = 0.28;
        float alpha = legacy ? 0.56F : 0.34F;
        int color = regionColor(shape.id());
        float red = ((color >> 16) & 0xFF) / 255F;
        float green = ((color >> 8) & 0xFF) / 255F;
        float blue = (color & 0xFF) / 255F;

        // BufferSource may reuse one BufferBuilder for non-fixed render types. Finish one type before requesting the
        // next, otherwise getBuffer(debugFilledBox) can close the still-referenced lines consumer.
        VertexConsumer mask = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.debugFilledBox());
        for (ChunkGroupShape.Cell cell : shape.boundaryCells()) {
            ChunkGroupBounds bounds = ChunkGroupBounds.forGroup(cell.x(), cell.z(), shape.groupChunks());
            int edges = shape.boundaryMask(cell);
            if ((edges & ChunkGroupShape.WEST) != 0) addWall(pose, mask, bounds.minX(), bottom, bounds.minZ(),
                    bounds.minX() + thickness, top, bounds.maxZ(), alpha, red, green, blue);
            if ((edges & ChunkGroupShape.EAST) != 0) addWall(pose, mask, bounds.maxX() - thickness, bottom, bounds.minZ(),
                    bounds.maxX(), top, bounds.maxZ(), alpha, red, green, blue);
            if ((edges & ChunkGroupShape.NORTH) != 0) addWall(pose, mask, bounds.minX(), bottom, bounds.minZ(),
                    bounds.maxX(), top, bounds.minZ() + thickness, alpha, red, green, blue);
            if ((edges & ChunkGroupShape.SOUTH) != 0) addWall(pose, mask, bounds.minX(), bottom, bounds.maxZ() - thickness,
                    bounds.maxX(), top, bounds.maxZ(), alpha, red, green, blue);
        }
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.debugFilledBox());
        pose.popPose();
    }

    private static void addWall(PoseStack pose, VertexConsumer consumer, double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ, float alpha, float red, float green, float blue) {
        LevelRenderer.addChainedFilledBoxVertices(pose, consumer, minX, minY, minZ, maxX, maxY, maxZ,
                red, green, blue, alpha);
    }

    /** One colour per region, matching the strategic-map palette. */
    private static int regionColor(String id) {
        int[] palette = {0x79A4A8, 0xAE936D, 0x729176, 0xA3787B, 0x8984A8, 0xA3A073, 0x709590, 0xA27B96};
        return palette[Math.floorMod(id.hashCode(), palette.length)];
    }
}
