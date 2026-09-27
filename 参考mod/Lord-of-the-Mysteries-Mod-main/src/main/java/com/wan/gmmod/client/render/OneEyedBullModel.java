package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.OneEyedBullEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OneEyedBullModel extends GeoModel<OneEyedBullEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/one_eyed_bull.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/one_eyed_bull.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/one_eyed_bull.animation.json");

    @Override
    public ResourceLocation getModelResource(OneEyedBullEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(OneEyedBullEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(OneEyedBullEntity animatable) { return ANIMATION; }
}
