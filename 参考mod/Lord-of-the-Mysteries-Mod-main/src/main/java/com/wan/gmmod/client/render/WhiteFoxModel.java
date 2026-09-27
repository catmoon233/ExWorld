package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.WhiteFoxEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WhiteFoxModel extends GeoModel<WhiteFoxEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/white_fox.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/white_fox.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/white_fox.animation.json");

    @Override
    public ResourceLocation getModelResource(WhiteFoxEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(WhiteFoxEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(WhiteFoxEntity animatable) { return ANIMATION; }
}