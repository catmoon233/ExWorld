package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.WolfmanEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 狼人的 GeckoLib 模型资源定位。
 * <p>
 * 对应资源（用户提供）：
 * <ul>
 *     <li>模型：{@code assets/guimi_mod/geo/wolfman.geo.json}</li>
 *     <li>贴图：{@code assets/guimi_mod/textures/entity/wolfman.png}</li>
 *     <li>动画：{@code assets/guimi_mod/animations/wolfman.animation.json}
 *     （animation.chase 四足扑击奔袭 / animation.attack 挥爪攻击）</li>
 * </ul>
 */
public class WolfmanModel extends GeoModel<WolfmanEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/wolfman.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/wolfman.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/wolfman.animation.json");

    @Override
    public ResourceLocation getModelResource(WolfmanEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(WolfmanEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WolfmanEntity animatable) {
        return ANIMATION;
    }
}
