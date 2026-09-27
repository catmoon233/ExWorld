package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.HornachisGoatEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HornachisGoatModel extends GeoModel<HornachisGoatEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/hornachis_goat.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/hornachis_goat.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/hornachis_goat.animation.json");

    @Override
    public ResourceLocation getModelResource(HornachisGoatEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(HornachisGoatEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(HornachisGoatEntity animatable) { return ANIMATION; }
}