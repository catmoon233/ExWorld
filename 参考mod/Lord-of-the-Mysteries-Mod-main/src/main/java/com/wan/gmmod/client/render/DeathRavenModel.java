package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.DeathRavenEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DeathRavenModel extends GeoModel<DeathRavenEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/death_raven.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/death_raven.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/death_raven.animation.json");

    @Override
    public ResourceLocation getModelResource(DeathRavenEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(DeathRavenEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(DeathRavenEntity animatable) { return ANIMATION; }
}
