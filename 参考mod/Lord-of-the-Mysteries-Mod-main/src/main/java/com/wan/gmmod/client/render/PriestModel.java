package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.PriestEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 神父的 GeckoLib 模型资源定位。
 * <p>
 * 相关资源（均由用户提供）：
 * <ul>
 *     <li>模型：{@code assets/guimi_mod/geo/priest.geo.json}</li>
 *     <li>贴图：{@code assets/guimi_mod/textures/entity/priest.png}</li>
 *     <li>动画：{@code assets/guimi_mod/animations/priest_walk.animation.json}（动画名 {@code animation}）</li>
 * </ul>
 */
public class PriestModel extends GeoModel<PriestEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/priest.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/priest.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/priest_walk.animation.json");

    @Override
    public ResourceLocation getModelResource(PriestEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PriestEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(PriestEntity animatable) {
        return ANIMATION;
    }
}