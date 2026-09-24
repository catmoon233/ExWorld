package io.redspace.irons_artifice.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.irons_artifice.IronsArtifice;
import io.redspace.irons_artifice.entity.ChainEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ChainEntityRenderer extends EntityRenderer<ChainEntity> {
    public static final ResourceLocation CHAIN_TEXTURE = IronsArtifice.id("textures/entity/entity_chain.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(CHAIN_TEXTURE);

    public ChainEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    @Override
    public void render(ChainEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        LivingEntity first = entity.getFirst();
        LivingEntity second = entity.getSecond();
        if (first == null || second == null || first.isRemoved() || second.isRemoved()) {
            return;
        }
        Vec3 entityPos = new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ())
        );
        float warmup = Mth.clamp((entity.warmup + partialTick) / ChainEntity.VISUAL_WARMUP_TIME, 0f, 1f);
        renderChainBetween(
                lerpCenter(first, partialTick).subtract(entityPos),
                lerpCenter(second, partialTick).subtract(entityPos),
                poseStack,
                bufferSource.getBuffer(RENDER_TYPE),
                packedLight,
                warmup
        );
    }

    @Override
    public ResourceLocation getTextureLocation(ChainEntity entity) {
        return CHAIN_TEXTURE;
    }

    @Override
    public boolean shouldRender(ChainEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    private static Vec3 lerpCenter(LivingEntity entity, float partialTick) {
        double x = Mth.lerp(partialTick, entity.xo, entity.getX());
        double y = Mth.lerp(partialTick, entity.yo, entity.getY());
        double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
        return new Vec3(x, y, z).add(0, entity.getBbHeight() * 0.5, 0);
    }

    private static void renderChainBetween(Vec3 start, Vec3 end, PoseStack poseStack, VertexConsumer buffer, int packedLight, float warmupPercent) {
        Vec3 delta = end.subtract(start);
        float distance = (float) delta.length();
        if (distance < 1.0E-4f) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);
        Vec3 direction = delta.normalize();
        poseStack.mulPose(new Quaternionf().rotationTo(
                new Vector3f(0, 0, 1),
                new Vector3f((float) direction.x, (float) direction.y, (float) direction.z)
        ));

        Vec3 destination = new Vec3(0, 0, distance);
        Vec3 origin = warmupPercent < 1f ? destination.scale(1f - warmupPercent) : Vec3.ZERO;
        PoseStack.Pose pose = poseStack.last();
        drawQuad(origin, destination, true, pose, buffer, packedLight, 0f, distance);
        drawQuad(origin, destination, false, pose, buffer, packedLight, 0f, distance);
        poseStack.popPose();
    }

    private static void drawQuad(Vec3 from, Vec3 to, boolean major, PoseStack.Pose pose, VertexConsumer consumer, int packedLight, float uvMin, float uvMax) {
        float width = 3 / 16f;
        float halfWidth;
        float halfHeight;
        if (major) {
            halfWidth = width * 0.5f;
            halfHeight = 0;
        } else {
            halfHeight = width * 0.5f;
            halfWidth = 0;
        }
        float uMin = 0;
        float uMax = width;
        if (!major) {
            uMin += width;
            uMax += width;
        }
        consumer.addVertex(pose, (float) from.x + halfWidth, (float) from.y + halfHeight, (float) from.z)
                .setColor(-1).setUv(uMax, uvMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, (float) from.x - halfWidth, (float) from.y - halfHeight, (float) from.z)
                .setColor(-1).setUv(uMin, uvMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, (float) to.x - halfWidth, (float) to.y - halfHeight, (float) to.z)
                .setColor(-1).setUv(uMin, uvMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, (float) to.x + halfWidth, (float) to.y + halfHeight, (float) to.z)
                .setColor(-1).setUv(uMax, uvMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, (float) from.x - halfWidth, (float) from.y - halfHeight, (float) from.z)
                .setColor(-1).setUv(uMin, uvMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, (float) from.x + halfWidth, (float) from.y + halfHeight, (float) from.z)
                .setColor(-1).setUv(uMax, uvMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, (float) to.x + halfWidth, (float) to.y + halfHeight, (float) to.z)
                .setColor(-1).setUv(uMax, uvMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, (float) to.x - halfWidth, (float) to.y - halfHeight, (float) to.z)
                .setColor(-1).setUv(uMin, uvMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
    }
}
