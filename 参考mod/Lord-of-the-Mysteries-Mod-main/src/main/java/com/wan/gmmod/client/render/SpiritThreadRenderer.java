package com.wan.gmmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.SpiritThreadClientState;
import com.wan.gmmod.client.SpiritVisionClient;
import com.wan.gmmod.content.abilities.SkillManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * 灵体之线渲染层（仅客户端）。
 * <p>
 * 仅当本地玩家已解锁秘偶大师「灵体之线视野」能力（{@code spirit_thread_vision}）、
 * 开启灵体之线视野（{@link SpiritVisionClient#isActive()}）且
 * {@link SpiritThreadClientState} 存在有效的秘偶 / 操控目标时，
 * 在世界渲染阶段 {@link RenderLevelStageEvent.Stage#AFTER_TRANSLUCENT_BLOCKS} 中，
 * 从玩家绘制一条始终朝向摄像机、带纹理的丝带到秘偶 / 目标身上。
 * <p>
 * 普通 V 键灵视（未解锁该能力的非秘偶大师）不会看到灵体之线。
 * <p>
 * 纹理为 {@code guimi_mod:textures/entity/spirit_thread.png}（沿长度平铺，玩家自行添加）；
 * 挣扎时线变红并沿路径左右抖动，凸显目标正在反抗。
 */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public final class SpiritThreadRenderer {
    /** 灵体之线纹理（玩家自行添加，缺失时显示为「缺失纹理」而不崩溃） */
    private static final ResourceLocation THREAD_TEXTURE =
            GuimiMod.id("textures/entity/spirit_thread.png");
    /** 灵体之线的可自定义渲染类型：半透明、双面、随视角混合 */
    private static final RenderType THREAD_RENDER_TYPE = RenderType.entityCutoutNoCull(THREAD_TEXTURE);
    /** 丝带半宽（米） */
    private static final float HALF_WIDTH = 0.06F;
    /** 纹理沿长度每米平铺的次数 */
    private static final float TILES_PER_METER = 4.0F;
    /** 挣扎时的抖动幅度（米） */
    private static final float STRUGGLE_AMPLITUDE = 0.18F;

    /** 灵体之线视野能力 id：仅已解锁该秘偶大师能力的玩家才能看到灵体之线 */
    private static final ResourceLocation THREAD_VISION_ABILITY = GuimiMod.id("spirit_thread_vision");

    private SpiritThreadRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        // 灵体之线只有在灵视 / 灵体之线视野下才可见
        if (!SpiritVisionClient.isActive() || !SpiritThreadClientState.isFresh()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        // 灵体之线纹理只在秘偶大师的「灵体之线视野」技能下可见（而非普通 V 键灵视）
        if (!SkillManager.isUnlocked(mc.player, THREAD_VISION_ABILITY)) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        boolean struggling = SpiritThreadClientState.isStruggling();
        Vec3 origin = mc.player.getEyePosition(partial);

        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(THREAD_RENDER_TYPE);

        // 秘偶之线（操控 / 已成秘偶）：稳定的灵青色
        Entity marionette = SpiritThreadClientState.getMarionette();
        if (marionette != null && marionette != mc.player) {
            drawThread(poseStack, consumer, cam, origin,
                    marionette.getPosition(partial).add(0, marionette.getBbHeight() * 0.5, 0),
                    camera, false, partial);
        }
        // 操控目标之线：正在被灵体之线拉扯的目标，挣扎时变红抖动
        Entity target = SpiritThreadClientState.getTarget();
        if (target != null && target != marionette && target != mc.player) {
            drawThread(poseStack, consumer, cam, origin,
                    target.getPosition(partial).add(0, target.getBbHeight() * 0.5, 0),
                    camera, struggling, partial);
        }
        buffers.endBatch(THREAD_RENDER_TYPE);
    }

    /** 绘制一条朝向摄像机的带纹理丝带（相机相对空间）。 */
    private static void drawThread(PoseStack poseStack, VertexConsumer consumer, Vec3 cam,
                                   Vec3 from, Vec3 to, Camera camera, boolean struggling, float partial) {
        Vec3 start = from.subtract(cam);
        Vec3 end = to.subtract(cam);
        Vec3 axis = end.subtract(start);
        double length = axis.length();
        if (length < 1.0E-3) {
            return;
        }
        Vec3 dir = axis.scale(1.0 / length);
        // 朝向摄像机的横向：线方向 × 摄像机视线
        Vec3 view = new Vec3(camera.getLookVector().x, camera.getLookVector().y, camera.getLookVector().z);
        Vec3 side = dir.cross(view);
        if (side.lengthSqr() < 1.0E-4) {
            side = dir.cross(new Vec3(0, 1, 0));
        }
        side = side.normalize().scale(HALF_WIDTH);

        // 挣扎：整条线沿横向做时间正弦抖动，凸显反抗
        if (struggling) {
            long time = System.currentTimeMillis();
            double wobble = Mth.sin((time % 1000L) / 1000.0F * (float) (Math.PI * 2.0) * 3.0F)
                    * STRUGGLE_AMPLITUDE;
            Vec3 offset = side.normalize().scale(wobble);
            start = start.add(offset);
            end = end.add(offset.scale(-1.0));
        }

        float r = struggling ? 1.0F : 0.47F;
        float g = struggling ? 0.25F : 1.0F;
        float b = struggling ? 0.25F : 0.93F;
        float a = 0.85F;
        float v1 = (float) length * TILES_PER_METER;
        int light = 0x00F000F0; // 满亮，不受世界光照影响

        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        // 双面：正反各一个四边形，避免背面被剔除
        quad(consumer, matrix, start, end, side, v1, r, g, b, a, light);
        quad(consumer, matrix, end, start, side.scale(-1.0), v1, r, g, b, a, light);
        poseStack.popPose();
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Vec3 s, Vec3 e, Vec3 side,
                             float v1, float r, float g, float b, float a, int light) {
        vertex(consumer, matrix, s.x + side.x, s.y + side.y, s.z + side.z, 0.0F, 0.0F, r, g, b, a, light);
        vertex(consumer, matrix, s.x - side.x, s.y - side.y, s.z - side.z, 1.0F, 0.0F, r, g, b, a, light);
        vertex(consumer, matrix, e.x - side.x, e.y - side.y, e.z - side.z, 1.0F, v1, r, g, b, a, light);
        vertex(consumer, matrix, e.x + side.x, e.y + side.y, e.z + side.z, 0.0F, v1, r, g, b, a, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, double x, double y, double z,
                               float u, float v, float r, float g, float b, float a, int light) {
        consumer.addVertex(matrix, (float) x, (float) y, (float) z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(0)
                .setLight(light)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
