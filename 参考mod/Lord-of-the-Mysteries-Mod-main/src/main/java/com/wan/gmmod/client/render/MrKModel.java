package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.MrKEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MrKModel extends GeoModel<MrKEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/mr_k.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/mr_k.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/mr_k.animation.json");

    @Override
    public ResourceLocation getModelResource(MrKEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(MrKEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(MrKEntity animatable) { return ANIMATION; }
}