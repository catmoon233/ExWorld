package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.EvilPantherEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EvilPantherModel extends GeoModel<EvilPantherEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/evil_panther.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/evil_panther.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/evil_panther.animation.json");

    @Override
    public ResourceLocation getModelResource(EvilPantherEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(EvilPantherEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(EvilPantherEntity animatable) { return ANIMATION; }
}