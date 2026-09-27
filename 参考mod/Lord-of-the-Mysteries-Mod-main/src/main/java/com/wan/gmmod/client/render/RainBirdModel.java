package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.RainBirdEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RainBirdModel extends GeoModel<RainBirdEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/rain_bird.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/rain_bird.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/rain_bird.animation.json");

    @Override
    public ResourceLocation getModelResource(RainBirdEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(RainBirdEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(RainBirdEntity animatable) { return ANIMATION; }
}
