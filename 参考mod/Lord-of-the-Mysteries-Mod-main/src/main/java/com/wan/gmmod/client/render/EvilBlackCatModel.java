package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.EvilBlackCatEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EvilBlackCatModel extends GeoModel<EvilBlackCatEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/evil_black_cat.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/evil_black_cat.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/evil_black_cat.animation.json");

    @Override
    public ResourceLocation getModelResource(EvilBlackCatEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(EvilBlackCatEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(EvilBlackCatEntity animatable) { return ANIMATION; }
}
