package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.WolfmanFormItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 狼人化叠加模型的 GeckoLib 资源定位（用户提供的「狼人化」模型）。
 * <p>
 * 资源：
 * <ul>
 *     <li>模型：{@code geo/wolfman_form.geo.json}（已镜像左右手臂，使利爪跟随玩家右手/左手）</li>
 *     <li>贴图：{@code textures/entity/wolfman_form.png}</li>
 *     <li>动画：空动画（骨骼姿态由 {@code applyBaseTransformations} 绑定玩家身体）</li>
 * </ul>
 */
public class WolfmanFormModel extends GeoModel<WolfmanFormItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/wolfman_form.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/wolfman_form.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/wolfman_form.animation.json");

    @Override
    public ResourceLocation getModelResource(WolfmanFormItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(WolfmanFormItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WolfmanFormItem animatable) {
        return ANIMATION;
    }
}
