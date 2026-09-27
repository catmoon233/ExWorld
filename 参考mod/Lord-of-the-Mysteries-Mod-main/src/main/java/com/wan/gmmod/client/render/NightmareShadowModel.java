package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.NightmareShadowEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NightmareShadowModel extends GeoModel<NightmareShadowEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/nightmare_shadow.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/nightmare_shadow.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/nightmare_shadow.animation.json");

    @Override
    public ResourceLocation getModelResource(NightmareShadowEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(NightmareShadowEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(NightmareShadowEntity animatable) { return ANIMATION; }
}
