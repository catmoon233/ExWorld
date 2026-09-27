package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.PendulumItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 黄水晶灵摆的 GeckoLib 模型资源定位。
 * <p>
 * 以下资源由用户自行提供：
 * <ul>
 *     <li>模型：{@code assets/guimi_mod/geo/pendulum.geo.json}</li>
 *     <li>贴图：{@code assets/guimi_mod/textures/item/pendulum.png}</li>
 *     <li>动画：{@code assets/guimi_mod/animations/pendulum.animation.json}
 *     （需包含：animation.pendulum.idle 待机、
 *     animation.pendulum.clockwise 顺时针（好）、
 *     animation.pendulum.counterclockwise 逆时针（坏）、
 *     animation.pendulum.still 静止（不好不坏））</li>
 * </ul>
 */
public class PendulumModel extends GeoModel<PendulumItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/pendulum.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/item/pendulum.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/pendulum.animation.json");

    @Override
    public ResourceLocation getModelResource(PendulumItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PendulumItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(PendulumItem animatable) {
        return ANIMATION;
    }
}
