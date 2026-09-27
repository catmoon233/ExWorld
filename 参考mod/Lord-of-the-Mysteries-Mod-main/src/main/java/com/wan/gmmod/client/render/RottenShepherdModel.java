package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.RottenShepherdEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RottenShepherdModel extends GeoModel<RottenShepherdEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/rotten_shepherd.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/rotten_shepherd.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/rotten_shepherd.animation.json");

    @Override
    public ResourceLocation getModelResource(RottenShepherdEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(RottenShepherdEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(RottenShepherdEntity animatable) { return ANIMATION; }
}
