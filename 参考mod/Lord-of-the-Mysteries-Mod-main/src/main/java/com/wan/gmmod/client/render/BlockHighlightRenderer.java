package com.wan.gmmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.BlockHighlightClientState;
import com.wan.gmmod.client.DistortionClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 方块高亮描边渲染层（仅客户端）。
 * <p>
 * 在世界渲染阶段 {@link RenderLevelStageEvent.Stage#AFTER_TRANSLUCENT_BLOCKS} 中，
 * 遍历 {@link BlockHighlightClientState} 记录的方块，为每个方块绘制金色描边线框。
 * 线框坐标需减去相机位置以转换到相机相对空间。
 * <p>
 * 该效果由地理占卜「清晰感知」触发（服务端发送 {@code HighlightBlocksPacket}），
 * 实现持续一段时间的矿石 / 宝箱透视描边。
 */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public final class BlockHighlightRenderer {
    // 金色描边（矿石 / 宝箱）
    private static final float R = 1.0F;
    private static final float G = 0.85F;
    private static final float B = 0.2F;
    private static final float A = 1.0F;
    // 淡紫色描边（扭曲区域边界）
    private static final float DR = 0.75F;
    private static final float DG = 0.45F;
    private static final float DB = 0.9F;
    private static final float DA = 1.0F;

    private BlockHighlightRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (BlockHighlightClientState.positions().isEmpty()
                && DistortionClientState.positions().isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        // 平移到相机相对空间
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        for (BlockPos pos : BlockHighlightClientState.positions()) {
            LevelRenderer.renderLineBox(
                    poseStack, consumer,
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0,
                    R, G, B, A);
        }

        for (BlockPos pos : DistortionClientState.positions()) {
            LevelRenderer.renderLineBox(
                    poseStack, consumer,
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0,
                    DR, DG, DB, DA);
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }
}
