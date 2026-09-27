package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.ThousandFacedHunterEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ThousandFacedHunterModel extends GeoModel<ThousandFacedHunterEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/thousand_faced_hunter.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/thousand_faced_hunter.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/thousand_faced_hunter.animation.json");

    @Override
    public ResourceLocation getModelResource(ThousandFacedHunterEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(ThousandFacedHunterEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(ThousandFacedHunterEntity animatable) { return ANIMATION; }
}