package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.SilenceGunItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SilenceGunModel extends GeoModel<SilenceGunItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/silence_gun.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/item/silence_gun.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/silence_gun.animation.json");

    @Override
    public ResourceLocation getModelResource(SilenceGunItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SilenceGunItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SilenceGunItem animatable) {
        return ANIMATION;
    }
}
