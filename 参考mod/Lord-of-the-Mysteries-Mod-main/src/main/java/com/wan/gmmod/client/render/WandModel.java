package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.WandItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WandModel extends GeoModel<WandItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/wand.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/item/wand.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/wand.animation.json");

    @Override
    public ResourceLocation getModelResource(WandItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(WandItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WandItem animatable) {
        return ANIMATION;
    }
}