package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.DemonFormItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 恶魔化全身模型的 GeckoLib 资源定位（用户提供的「恶魔化」模型）。
 * <p>
 * 资源：
 * <ul>
 *     <li>模型：{@code geo/demon_form.geo.json}（x=8 / z=-8 中心的巨大恶魔，渲染时由
 *     {@link TransformVisualLayer} 平移归零并放大）</li>
 *     <li>贴图：{@code textures/entity/demon_form.png}</li>
 *     <li>动画：空动画（骨骼姿态由 {@code applyBaseTransformations} 绑定玩家身体）</li>
 * </ul>
 */
public class DemonFormModel extends GeoModel<DemonFormItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/demon_form.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/demon_form.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/demon_form.animation.json");

    @Override
    public ResourceLocation getModelResource(DemonFormItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DemonFormItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DemonFormItem animatable) {
        return ANIMATION;
    }
}
