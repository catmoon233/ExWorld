package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.AbyssDemonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AbyssDemonModel extends GeoModel<AbyssDemonEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/abyss_demon.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/abyss_demon.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/abyss_demon.animation.json");

    @Override
    public ResourceLocation getModelResource(AbyssDemonEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(AbyssDemonEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(AbyssDemonEntity animatable) { return ANIMATION; }
}