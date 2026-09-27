package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.BlackSpottedFrogEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BlackSpottedFrogModel extends GeoModel<BlackSpottedFrogEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/black_spotted_frog.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/black_spotted_frog.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/black_spotted_frog.animation.json");

    @Override
    public ResourceLocation getModelResource(BlackSpottedFrogEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(BlackSpottedFrogEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(BlackSpottedFrogEntity animatable) { return ANIMATION; }
}
