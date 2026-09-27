package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.HumanSkinShadowEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HumanSkinShadowModel extends GeoModel<HumanSkinShadowEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/human_skin_shadow.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/human_skin_shadow.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/human_skin_shadow.animation.json");

    @Override
    public ResourceLocation getModelResource(HumanSkinShadowEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(HumanSkinShadowEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(HumanSkinShadowEntity animatable) { return ANIMATION; }
}