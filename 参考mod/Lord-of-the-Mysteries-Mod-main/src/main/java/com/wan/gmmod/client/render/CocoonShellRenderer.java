package com.wan.gmmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.CocoonClientState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Map;

/**
 * 蛛丝蚕茧的世界渲染层（仅客户端）。
 * <p>
 * 在每个仍处于蚕茧包裹状态的实体周围绘制一个<b>纯白椭球体外壳</b>（完全包裹住人物）。
 * 用白色噪声纹理填充，透明度随剩余时长（尾段渐隐）变化；火破时短暂残留并偏红。
 */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public final class CocoonShellRenderer {

    /** 蚕茧外壳纹理（纯白色，用于椭球表面） */
    private static final ResourceLocation WHITE = GuimiMod.id("textures/effects/cocoon_white.png");

    /** 蚕茧初始剩余时长（与 CocoonAbility.DURATION 一致，用于归一化） */
    private static final float BASE_TICKS = 100.0F;
    /** 椭球体网格细分：纬线段数 × 经线段数 */
    private static final int LAT = 24;
    private static final int LON = 48;

    private CocoonShellRenderer() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        Map<Integer, CocoonClientState.Entry> actives = CocoonClientState.snapshot();
        if (actives.isEmpty()) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera cam = event.getCamera();
        Vec3 cameraPos = cam.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType shellType = RenderType.entityTranslucentCull(WHITE);

        for (Map.Entry<Integer, CocoonClientState.Entry> e : actives.entrySet()) {
            Entity entity = mc.level.getEntity(e.getKey());
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            CocoonClientState.Entry state = e.getValue();
            float ratio = Mth.clamp(state.remainingTicks() / BASE_TICKS, 0.0F, 1.0F);
            Vec3 center = living.getEyePosition(partial);

            VertexConsumer shell = buffers.getBuffer(shellType);
            float pulse = GameTimePulse();
            drawEllipsoidShell(poseStack, shell, cameraPos, center, ratio, state.burning(), pulse);
        }
        buffers.endBatch(shellType);
    }

    /** 以心跳脉动的正弦值，让烧毁/渐隐更有张力（0~1）。 */
    private static float GameTimePulse() {
        return (Mth.sin((Minecraft.getInstance().player.tickCount) * 0.2F) * 0.5F + 0.5F);
    }

    /** 绘制包裹住人物的纯白椭球体外壳（经纬细化后的实体表面）。 */
    private static void drawEllipsoidShell(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPos,
                                           Vec3 center, float ratio, boolean burning, float pulse) {
        float alpha = (0.52F * ratio + 0.10F) * (burning ? 0.35F + 0.65F * pulse : 1.0F);
        alpha = Mth.clamp(alpha, 0.0F, 0.92F);
        // 纯白，但随半径略扩以包裹体型；火破时转为暗红
        float cr = burning ? 0.9F : 1.0F;
        float cg = burning ? 0.3F : 1.0F;
        float cb = burning ? 0.3F : 1.0F;

        double cx = center.x - cameraPos.x;
        double cy = center.y - cameraPos.y;
        double cz = center.z - cameraPos.z;
        // 长轴沿人物高度（包裹整个身体），水平为圆形短轴
        float rx = 0.95F + 0.06F * ratio;
        float ry = 1.75F + 0.10F * ratio;
        float rz = rx * 0.95F;

        Matrix4f m = poseStack.last().pose();
        for (int lat = 0; lat < LAT; lat++) {
            double theta0 = lat * Math.PI / LAT;
            double theta1 = (lat + 1) * Math.PI / LAT;
            for (int lon = 0; lon < LON; lon++) {
                double phi0 = lon * Math.PI * 2.0 / LON;
                double phi1 = (lon + 1) * Math.PI * 2.0 / LON;
                sphereQuad(consumer, m,
                        cx(cx, rx, theta0, phi0), cy(cy, ry, theta0), cz(cz, rz, theta0, phi0), u0(lon), v0(lat),
                        cx(cx, rx, theta0, phi1), cy(cy, ry, theta0), cz(cz, rz, theta0, phi1), u1(lon), v0(lat),
                        cx(cx, rx, theta1, phi0), cy(cy, ry, theta1), cz(cz, rz, theta1, phi0), u0(lon), v1(lat),
                        cx(cx, rx, theta1, phi1), cy(cy, ry, theta1), cz(cz, rz, theta1, phi1), u1(lon), v1(lat),
                        cr, cg, cb, alpha);
            }
        }
    }

    private static float cx(double cx, float rx, double theta, double phi) {
        return (float) (cx + Math.sin(theta) * Math.cos(phi) * rx);
    }

    private static float cy(double cy, float ry, double theta) {
        return (float) (cy + Math.cos(theta) * ry);
    }

    private static float cz(double cz, float rz, double theta, double phi) {
        return (float) (cz + Math.sin(theta) * Math.sin(phi) * rz);
    }

    private static float u0(int lon) {
        return lon / (float) LON;
    }

    private static float u1(int lon) {
        return (lon + 1) / (float) LON;
    }

    private static float v0(int lat) {
        return lat / (float) LAT;
    }

    private static float v1(int lat) {
        return (lat + 1) / (float) LAT;
    }

    /** 绘制椭球表面上的一个小四边形（两块三角形）。 */
    private static void sphereQuad(VertexConsumer consumer, Matrix4f m,
                                   float ax, float ay, float az, float au, float av,
                                   float bx, float by, float bz, float bu, float bv,
                                   float cx1, float cy1, float cz1, float cu, float cv,
                                   float dx, float dy, float dz, float du, float dv,
                                   float r, float g, float b, float a) {
        tri(consumer, m, ax, ay, az, au, av, bx, by, bz, bu, bv, cx1, cy1, cz1, cu, cv, r, g, b, a);
        tri(consumer, m, ax, ay, az, au, av, cx1, cy1, cz1, cu, cv, dx, dy, dz, du, dv, r, g, b, a);
    }

    /** 绘制单个三角形。 */
    private static void tri(VertexConsumer consumer, Matrix4f m,
                            float ax, float ay, float az, float au, float av,
                            float bx, float by, float bz, float bu, float bv,
                            float cx, float cy, float cz, float cu, float cv,
                            float r, float g, float b, float a) {
        consumer.addVertex(m, ax, ay, az).setColor(r, g, b, a).setUv(au, av).setOverlay(0).setLight(0x00F000F0).setNormal(0, 1, 0);
        consumer.addVertex(m, bx, by, bz).setColor(r, g, b, a).setUv(bu, bv).setOverlay(0).setLight(0x00F000F0).setNormal(0, 1, 0);
        consumer.addVertex(m, cx, cy, cz).setColor(r, g, b, a).setUv(cu, cv).setOverlay(0).setLight(0x00F000F0).setNormal(0, 1, 0);
    }
}