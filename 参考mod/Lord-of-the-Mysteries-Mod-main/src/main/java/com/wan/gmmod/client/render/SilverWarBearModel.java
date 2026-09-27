package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.SilverWarBearEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** 银白战熊的 GeckoLib 模型资源定位。 */
public class SilverWarBearModel extends GeoModel<SilverWarBearEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/silver_war_bear.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/silver_war_bear.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/silver_war_bear.animation.json");

    @Override
    public ResourceLocation getModelResource(SilverWarBearEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SilverWarBearEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SilverWarBearEntity animatable) {
        return ANIMATION;
    }
}
