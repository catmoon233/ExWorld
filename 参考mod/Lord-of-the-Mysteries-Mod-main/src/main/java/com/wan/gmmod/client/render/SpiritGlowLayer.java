package com.wan.gmmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.SpiritVisionClient;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;

/**
 * 灵体光晕层（仅客户端）。
 * <p>
 * 追加到灵体实体（{@code SpiritBeing}）的渲染器上：开启灵视时，为灵体叠加
 * 两层半透明的幽蓝紫能量外壳（外层淡光晕 + 内层亮核心），形象地表现「灵体发光」；
 * 多个灵体聚集处自然形成一片彩色光晕，即「灵性浓度区域的彩色光晕」。
 */
public class SpiritGlowLayer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {
    /** 与灵体之线共用的发光线理（能量外壳随模型而虚化流动） */
    private static final ResourceLocation THREAD_TEXTURE =
            GuimiMod.id("textures/entity/spirit_thread.png");
    /** 外层光晕相对模型放大倍数 */
    private static final float OUTER_INFLATE = 1.22F;
    /** 内层核心相对模型放大倍数 */
    private static final float INNER_INFLATE = 1.08F;
    /** 外层光晕颜色：淡透蓝紫 */
    private static final int OUTER_COLOR = FastColor.ARGB32.color(90, 140, 200, 255);
    /** 内层核心颜色：亮白蓝 */
    private static final int INNER_COLOR = FastColor.ARGB32.color(160, 190, 240, 255);

    public SpiritGlowLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!SpiritVisionClient.isActive()) {
            return;
        }
        float scroll = (entity.tickCount + partialTick) * 0.012F;
        poseStack.pushPose();
        // 外层光晕：放大、更淡
        poseStack.pushPose();
        poseStack.scale(OUTER_INFLATE, OUTER_INFLATE, OUTER_INFLATE);
        VertexConsumer outer = buffer.getBuffer(
                RenderType.energySwirl(THREAD_TEXTURE, scroll % 1.0F, scroll % 1.0F));
        getParentModel().renderToBuffer(poseStack, outer, packedLight, OverlayTexture.NO_OVERLAY, OUTER_COLOR);
        poseStack.popPose();
        // 内层核心：亮白蓝贴体
        poseStack.scale(INNER_INFLATE, INNER_INFLATE, INNER_INFLATE);
        VertexConsumer inner = buffer.getBuffer(
                RenderType.energySwirl(THREAD_TEXTURE, (scroll + 0.5F) % 1.0F, (scroll + 0.5F) % 1.0F));
        getParentModel().renderToBuffer(poseStack, inner, packedLight, OverlayTexture.NO_OVERLAY, INNER_COLOR);
        poseStack.popPose();
    }
}