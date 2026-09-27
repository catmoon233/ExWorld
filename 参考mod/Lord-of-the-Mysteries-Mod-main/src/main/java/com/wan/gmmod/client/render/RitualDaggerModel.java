package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.RitualDaggerItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RitualDaggerModel extends GeoModel<RitualDaggerItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/ritual_dagger.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/item/ritual_dagger.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/ritual_dagger.animation.json");

    @Override
    public ResourceLocation getModelResource(RitualDaggerItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RitualDaggerItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RitualDaggerItem animatable) {
        return ANIMATION;
    }
}
