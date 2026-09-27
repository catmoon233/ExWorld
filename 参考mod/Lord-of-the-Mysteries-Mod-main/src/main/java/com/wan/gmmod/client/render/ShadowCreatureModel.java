package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.ShadowCreatureEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 阴影生物的 GeckoLib 模型资源定位。
 * <p>
 * 对应资源（用户提供）：
 * <ul>
 *     <li>模型：{@code assets/guimi_mod/geo/shadow_creature.geo.json}</li>
 *     <li>贴图：{@code assets/guimi_mod/textures/entity/shadow_creature.png}</li>
 *     <li>动画：{@code assets/guimi_mod/animations/shadow_creature.animation.json}
 *     （animation.walk 循环走动 / animation.attack 攻击）</li>
 * </ul>
 */
public class ShadowCreatureModel extends GeoModel<ShadowCreatureEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/shadow_creature.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/shadow_creature.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/shadow_creature.animation.json");

    @Override
    public ResourceLocation getModelResource(ShadowCreatureEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ShadowCreatureEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ShadowCreatureEntity animatable) {
        return ANIMATION;
    }
}