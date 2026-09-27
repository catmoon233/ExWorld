package com.wan.gmmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wan.gmmod.content.entities.FlyingCardEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * 飞牌渲染器：纸牌以「平放」姿态飞行（卡面水平，而非默认投掷物的竖直广告牌），
 * 并绕自身竖直法线快速自旋，模拟真实掷牌的旋转轨迹。
 */
public class FlyingCardRenderer extends EntityRenderer<FlyingCardEntity> {
    /** 自旋角速度（度 / 刻） */
    private static final float SPIN_SPEED = 30.0F;

    private final ItemRenderer itemRenderer;

    public FlyingCardRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(FlyingCardEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.1, 0.0);
        // 先绕世界竖直轴自旋（含飞行朝向），再绕 X 轴 90° 把卡面放平
        float spin = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot())
                + (entity.tickCount + partialTicks) * SPIN_SPEED;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        this.itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingCardEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
