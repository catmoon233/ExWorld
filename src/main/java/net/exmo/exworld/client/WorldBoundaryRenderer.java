package net.exmo.exworld.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.exmo.exworld.world.generation.IslandLayout;
import net.exmo.exworld.Config;
import net.exmo.exworld.client.battle.BattleClient;
import net.exmo.exworld.world.model.ChunkGroupShape;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Renders one continuous outer slab per straight region edge, and only inside the render distance. */
public final class WorldBoundaryRenderer {
    private static final double HEIGHT_SCALE = 0.7;
    private WorldBoundaryRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (BattleClient.active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.level.dimension() != Level.OVERWORLD) return;
        if (ChunkGroupRenderCuller.bypassBoundary()) return;
        ChunkGroupShape shape = ClientChunkGroupState.active();
        if (shape == null || shape.isEmpty() || !shape.containsPosition(minecraft.player.getX(), minecraft.player.getZ())) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        boolean decryption = Config.decryptionMode;
        boolean legacy = Config.legacyRegionBoundary;
        double bottom;
        double top;
        if (decryption || legacy) {
            // Fixed full build height. Decryption mode must not follow the player.
            bottom = minecraft.level.getMinBuildHeight();
            top = minecraft.level.getMaxBuildHeight();
        } else if (ClientChunkGroupState.archipelago()) {
            // A fixed band at the island-top level, never following the player up and down.
            bottom = IslandLayout.DEFAULT_MIN_Y;
            top = IslandLayout.DEFAULT_MIN_Y + 5.0;
        } else {
            bottom = minecraft.player.getY() - 2.0;
            top = minecraft.player.getY() + 3.0;
        }
        top = bottom + (top - bottom) * HEIGHT_SCALE;
        double thickness = 0.28;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 background = backgroundColor(minecraft, event.getCamera(), partialTick);
        float alpha = 0.10F;
        float red = (float) background.x;
        float green = (float) background.y;
        float blue = (float) background.z;

        int renderDistance = minecraft.options.getEffectiveRenderDistance();
        int originX = Mth.floor(camera.x) >> 4;
        int originZ = Mth.floor(camera.z) >> 4;
        double windowMinX = (originX - renderDistance) * 16.0;
        double windowMaxX = (originX + renderDistance + 1) * 16.0;
        double windowMinZ = (originZ - renderDistance) * 16.0;
        double windowMaxZ = (originZ + renderDistance + 1) * 16.0;
        double wallTop = top;
        List<BoundarySlabs.Slab> slabs = new ArrayList<>();
        if (top - bottom > thickness + 0.02) {
            wallTop = top - thickness - 0.01;
            slabs.addAll(BoundarySlabs.caps(shape, top, thickness,
                    windowMinX, windowMinZ, windowMaxX, windowMaxZ));
        }
        slabs.addAll(BoundarySlabs.visible(shape, bottom, wallTop, thickness,
                windowMinX, windowMinZ, windowMaxX, windowMaxZ));
        if (!slabs.isEmpty()) {
            VertexConsumer mask = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.debugFilledBox());
            for (BoundarySlabs.Slab slab : slabs) {
                addWall(pose, mask, slab.minX(), slab.minY(), slab.minZ(), slab.maxX(), slab.maxY(), slab.maxZ(),
                        alpha, red, green, blue);
            }
            minecraft.renderBuffers().bufferSource().endBatch(RenderType.debugFilledBox());
        }
        pose.popPose();
    }

    private static void addWall(PoseStack pose, VertexConsumer consumer, double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ, float alpha, float red, float green, float blue) {
        LevelRenderer.addChainedFilledBoxVertices(pose, consumer, minX, minY, minZ, maxX, maxY, maxZ,
                red, green, blue, alpha);
    }

    /** Current fog is the distance background and already follows time, weather and biome. Sky fills the rest. */
    private static Vec3 backgroundColor(Minecraft minecraft, Camera camera, float partialTick) {
        Vec3 sky = minecraft.level.getSkyColor(camera.getPosition(), partialTick);
        float[] fog = RenderSystem.getShaderFogColor();
        if (fog[0] + fog[1] + fog[2] <= 0.02F) return sky;
        return new Vec3(
                Mth.lerp(0.35, fog[0], sky.x),
                Mth.lerp(0.35, fog[1], sky.y),
                Mth.lerp(0.35, fog[2], sky.z));
    }
}
