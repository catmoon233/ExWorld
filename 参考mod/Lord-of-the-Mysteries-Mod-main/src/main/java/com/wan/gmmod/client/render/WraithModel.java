package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.WraithEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 怨灵的 GeckoLib 模型资源定位。
 * <p>
 * 以下资源由用户自行提供：
 * <ul>
 *     <li>模型：{@code assets/guimi_mod/geo/wraith.geo.json}</li>
 *     <li>贴图：{@code assets/guimi_mod/textures/entity/wraith.png}</li>
 *     <li>动画：{@code assets/guimi_mod/animations/wraith.animation.json}
 *     （需包含：animation.wraith.idle 待机（循环）、
 *     animation.wraith.attack 攻击）</li>
 * </ul>
 */
public class WraithModel extends GeoModel<WraithEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/wraith.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/wraith.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/wraith.animation.json");

    @Override
    public ResourceLocation getModelResource(WraithEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(WraithEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WraithEntity animatable) {
        return ANIMATION;
    }
}
