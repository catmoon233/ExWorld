package com.wan.gmmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wan.gmmod.common.item.DawnSwordItem;
import com.wan.gmmod.common.registry.ModEffects;
import com.wan.gmmod.common.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.util.InternalUtil;

/**
 * 黎明加持视觉层（仅客户端）。
 * <p>
 * 追加到玩家渲染器上：当隐藏标记效果生效时——
 * <ul>
 *   <li>{@code dawn_armor_active}：把「黎明命甲」的 GeckoLib 铠甲模型直接渲染到
 *       玩家身上（复用 geckolib 的盔甲渲染管线 {@link InternalUtil#tryRenderGeoArmorPiece}，
 *       骨骼姿势绑定玩家身体）；</li>
 *   <li>{@code dawn_sword_active}：在玩家主手渲染「晨曦之剑」的 GeckoLib 模型
 *       （若本身已手握晨曦之剑则跳过）。</li>
 * </ul>
 */
public class DawnBuffVisualLayer<T extends LivingEntity, M extends HumanoidModel<T>>
        extends RenderLayer<T, M> {
    private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

    public DawnBuffVisualLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        boolean armor = entity.hasEffect(ModEffects.DAWN_ARMOR_ACTIVE);
        boolean sword = entity.hasEffect(ModEffects.DAWN_SWORD_ACTIVE);
        if (!armor && !sword) {
            return;
        }
        if (armor) {
            // 黎明铠甲：GeckoLib 铠甲直接绑到玩家身体（胸部槽位盔甲件）
            ItemStack armorStack = new ItemStack(ModItems.DAWN_ARMOR.get());
            poseStack.pushPose();
            InternalUtil.tryRenderGeoArmorPiece(poseStack, buffer, entity, armorStack,
                    EquipmentSlot.CHEST, getParentModel(), getParentModel(),
                    partialTick, packedLight, limbSwing, limbSwingAmount, ageInTicks,
                    netHeadYaw, headPitch, (base, slot) -> {
                    });
            poseStack.popPose();
        }
        if (sword && !(entity.getMainHandItem().getItem() instanceof DawnSwordItem)) {
            // 晨曦之剑：在主手渲染光剑模型（第三人称手持姿态）
            ItemStack swordStack = new ItemStack(ModItems.DAWN_SWORD.get());
            poseStack.pushPose();
            getParentModel().rightArm.translateAndRotate(poseStack);
            this.itemRenderer.renderStatic(swordStack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer,
                    entity.level(), entity.getId());
            poseStack.popPose();
        }
    }
}