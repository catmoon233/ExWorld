package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.BlackScaleSharkEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BlackScaleSharkModel extends GeoModel<BlackScaleSharkEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/black_scale_shark.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/black_scale_shark.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/black_scale_shark.animation.json");

    @Override
    public ResourceLocation getModelResource(BlackScaleSharkEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(BlackScaleSharkEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(BlackScaleSharkEntity animatable) { return ANIMATION; }
}
