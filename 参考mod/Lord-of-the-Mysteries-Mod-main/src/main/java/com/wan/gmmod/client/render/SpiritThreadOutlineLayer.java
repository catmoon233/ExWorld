package com.wan.gmmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.SpiritVisionClient;
import com.wan.gmmod.content.abilities.SkillManager;
import net.minecraft.client.Minecraft;
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
 * 灵体之线轮廓层（仅客户端）。
 * <p>
 * 追加到所有活体实体渲染器上：仅当本地玩家已解锁秘偶大师「灵体之线视野」能力
 * （{@code spirit_thread_vision}）且视野开启时，在 {@value #RADIUS} 米内的生物
 * 轮廓周围渲染一层缓慢流动的灵体之线纹理（{@code guimi_mod:textures/entity/spirit_thread.png}），
 * 表现缠绕在灵体上的丝线；普通 V 键灵视不显示。
 * <p>
 * 实现方式与闪电苦力怕的电光层一致：以 {@link RenderType#energySwirl} 沿实体模型
 * 再渲染一层略微放大的半透明「外壳」，纹理随时间滚动。
 */
public class SpiritThreadOutlineLayer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {
    /** 灵体之线纹理（与世界内丝带共用，玩家自行添加） */
    private static final ResourceLocation THREAD_TEXTURE =
            GuimiMod.id("textures/entity/spirit_thread.png");
    /** 灵体之线视野能力 id：仅秘偶大师解锁后可见 */
    private static final ResourceLocation THREAD_VISION_ABILITY = GuimiMod.id("spirit_thread_vision");
    /** 轮廓显示半径（米），与灵体之线视野的反隐半径保持一致 */
    private static final double RADIUS = 30.0;
    /** 轮廓外壳相对模型的放大倍数，避免与皮肤纹理深度冲突 */
    private static final float INFLATE = 1.05F;
    /** 半透明灵青色（与世界内丝带同色系） */
    private static final int COLOR = FastColor.ARGB32.color(200, 120, 255, 238);

    public SpiritThreadOutlineLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity == mc.player) {
            return;
        }
        // 仅秘偶大师的「灵体之线视野」技能下可见（而非普通 V 键灵视）
        if (!SpiritVisionClient.isActive()
                || !SkillManager.isUnlocked(mc.player, THREAD_VISION_ABILITY)
                || mc.player.distanceTo(entity) > RADIUS) {
            return;
        }
        // 纹理随时间缓慢滚动，表现丝线沿灵体流转
        float scroll = (entity.tickCount + partialTick) * 0.008F;
        VertexConsumer consumer = buffer.getBuffer(
                RenderType.energySwirl(THREAD_TEXTURE, scroll % 1.0F, scroll % 1.0F));
        poseStack.pushPose();
        poseStack.scale(INFLATE, INFLATE, INFLATE);
        getParentModel().renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, COLOR);
        poseStack.popPose();
    }
}
