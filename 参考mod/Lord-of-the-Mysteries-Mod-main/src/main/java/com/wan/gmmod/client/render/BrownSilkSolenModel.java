package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.BrownSilkSolenEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BrownSilkSolenModel extends GeoModel<BrownSilkSolenEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/brown_silk_solen.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/brown_silk_solen.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/brown_silk_solen.animation.json");

    @Override
    public ResourceLocation getModelResource(BrownSilkSolenEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(BrownSilkSolenEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(BrownSilkSolenEntity animatable) { return ANIMATION; }
}