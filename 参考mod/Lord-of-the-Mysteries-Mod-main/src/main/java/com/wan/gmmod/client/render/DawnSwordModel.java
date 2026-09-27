package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.DawnSwordItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DawnSwordModel extends GeoModel<DawnSwordItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/dawn_sword.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/item/dawn_sword.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/dawn_sword.animation.json");

    @Override
    public ResourceLocation getModelResource(DawnSwordItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DawnSwordItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DawnSwordItem animatable) {
        return ANIMATION;
    }
}