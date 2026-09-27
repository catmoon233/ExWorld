package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.GrayBirdGrandmaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GrayBirdGrandmaModel extends GeoModel<GrayBirdGrandmaEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/gray_bird_grandma.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/gray_bird_grandma.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/gray_bird_grandma.animation.json");

    @Override
    public ResourceLocation getModelResource(GrayBirdGrandmaEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(GrayBirdGrandmaEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(GrayBirdGrandmaEntity animatable) { return ANIMATION; }
}
