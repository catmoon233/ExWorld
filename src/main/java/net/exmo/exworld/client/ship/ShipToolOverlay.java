package net.exmo.exworld.client.ship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** World overlay for the administrator capture box. */
public final class ShipToolOverlay {
    private static BlockPos a;
    private static BlockPos b;
    private static boolean present;

    private ShipToolOverlay() {}

    public static void install(BlockPos first, BlockPos second, boolean show) {
        a = first;
        b = second;
        present = show;
    }

    public static void render(RenderLevelStageEvent event) {
        if (!present || a == null || b == null) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        AABB box = new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
        VertexConsumer consumer = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, consumer, box, 0.85f, 0.72f, 0.35f, 1f);
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
        pose.popPose();
    }
}
