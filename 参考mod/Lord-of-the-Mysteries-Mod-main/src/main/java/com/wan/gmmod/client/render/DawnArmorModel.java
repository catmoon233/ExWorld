package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.DawnArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DawnArmorModel extends GeoModel<DawnArmorItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/dawn_armor.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/models/armor/dawn_armor.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/dawn_armor.animation.json");

    @Override
    public ResourceLocation getModelResource(DawnArmorItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DawnArmorItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DawnArmorItem animatable) {
        return ANIMATION;
    }
}