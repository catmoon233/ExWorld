package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.LavaDemonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class LavaDemonModel extends GeoModel<LavaDemonEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/lava_demon.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/lava_demon.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/lava_demon.animation.json");

    @Override
    public ResourceLocation getModelResource(LavaDemonEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(LavaDemonEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(LavaDemonEntity animatable) { return ANIMATION; }
}