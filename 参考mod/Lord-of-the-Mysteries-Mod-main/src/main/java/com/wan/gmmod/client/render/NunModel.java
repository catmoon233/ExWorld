package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.NunEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 修女的 GeckoLib 模型资源定位。
 * <p>
 * 相关资源（均由用户提供）：
 * <ul>
 *     <li>模型：{@code assets/guimi_mod/geo/nun.geo.json}</li>
 *     <li>贴图：{@code assets/guimi_mod/textures/entity/nun.png}</li>
 *     <li>动画：{@code assets/guimi_mod/animations/nun_walk.animation.json}（动画名 {@code animation}）</li>
 * </ul>
 */
public class NunModel extends GeoModel<NunEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/nun.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/nun.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/nun_walk.animation.json");

    @Override
    public ResourceLocation getModelResource(NunEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(NunEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(NunEntity animatable) {
        return ANIMATION;
    }
}