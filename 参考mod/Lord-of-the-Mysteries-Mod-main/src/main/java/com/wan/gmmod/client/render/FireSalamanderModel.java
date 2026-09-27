package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.FireSalamanderEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FireSalamanderModel extends GeoModel<FireSalamanderEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/fire_salamander.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/fire_salamander.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/fire_salamander.animation.json");

    @Override
    public ResourceLocation getModelResource(FireSalamanderEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(FireSalamanderEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(FireSalamanderEntity animatable) { return ANIMATION; }
}
