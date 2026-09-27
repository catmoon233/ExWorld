package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.WidowSpiderEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WidowSpiderModel extends GeoModel<WidowSpiderEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/widow_spider.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/widow_spider.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/widow_spider.animation.json");

    @Override
    public ResourceLocation getModelResource(WidowSpiderEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(WidowSpiderEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(WidowSpiderEntity animatable) { return ANIMATION; }
}