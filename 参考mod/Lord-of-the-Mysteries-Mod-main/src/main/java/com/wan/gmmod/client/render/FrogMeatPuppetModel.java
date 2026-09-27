package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.FrogMeatPuppetEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FrogMeatPuppetModel extends GeoModel<FrogMeatPuppetEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/frog_meat_puppet.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/frog_meat_puppet.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/frog_meat_puppet.animation.json");

    @Override
    public ResourceLocation getModelResource(FrogMeatPuppetEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(FrogMeatPuppetEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(FrogMeatPuppetEntity animatable) { return ANIMATION; }
}
