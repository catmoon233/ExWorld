package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.MagmaSwordItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MagmaSwordModel extends GeoModel<MagmaSwordItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/magma_sword.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/item/magma_sword.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/magma_sword.animation.json");

    @Override
    public ResourceLocation getModelResource(MagmaSwordItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MagmaSwordItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MagmaSwordItem animatable) {
        return ANIMATION;
    }
}