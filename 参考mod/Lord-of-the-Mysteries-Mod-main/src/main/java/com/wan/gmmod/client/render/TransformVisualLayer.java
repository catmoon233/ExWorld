package com.wan.gmmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wan.gmmod.common.registry.ModEffects;
import com.wan.gmmod.common.registry.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.util.InternalUtil;

/**
 * 变身视觉层（仅客户端）。
 * <p>
 * 追加到玩家渲染器上：当隐藏标记效果生效时——
 * <ul>
 *   <li>{@code werewolf_form}：渲染「狼人化」叠加模型（狼头 / 利爪 / 尾巴，双腿保持人形），
 *       骨骼姿态绑定玩家身体；</li>
 *   <li>{@code demon_form}：渲染「恶魔化」全身模型（巨大恶魔，带翅膀与犄角），
 *       模型以 x=8 / z=-8 为中心，渲染前先平移归零并放大 1.2 倍。</li>
 * </ul>
 */
public class TransformVisualLayer<T extends LivingEntity, M extends HumanoidModel<T>>
        extends RenderLayer<T, M> {

    public TransformVisualLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        boolean werewolf = entity.hasEffect(ModEffects.WEREWOLF_FORM);
        boolean demon = entity.hasEffect(ModEffects.DEMON_FORM);
        if (!werewolf && !demon) {
            return;
        }
        if (werewolf) {
            ItemStack stack = new ItemStack(ModItems.WOLFMAN_FORM.get());
            poseStack.pushPose();
            InternalUtil.tryRenderGeoArmorPiece(poseStack, buffer, entity, stack,
                    EquipmentSlot.CHEST, getParentModel(), getParentModel(),
                    partialTick, packedLight, limbSwing, limbSwingAmount, ageInTicks,
                    netHeadYaw, headPitch, (base, slot) -> {
                    });
            poseStack.popPose();
        }
        if (demon) {
            ItemStack stack = new ItemStack(ModItems.DEMON_FORM.get());
            poseStack.pushPose();
            // 恶魔化模型以 (8, z=-8) 为中心：放大 1.2 倍并把中心平移回玩家身体
            poseStack.scale(1.2f, 1.2f, 1.2f);
            poseStack.translate(-0.5f, 0.0f, 0.5f);
            InternalUtil.tryRenderGeoArmorPiece(poseStack, buffer, entity, stack,
                    EquipmentSlot.CHEST, getParentModel(), getParentModel(),
                    partialTick, packedLight, limbSwing, limbSwingAmount, ageInTicks,
                    netHeadYaw, headPitch, (base, slot) -> {
                    });
            poseStack.popPose();
        }
    }
}
