package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.VengefulShadowEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class VengefulShadowModel extends GeoModel<VengefulShadowEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/vengeful_shadow.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/vengeful_shadow.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/vengeful_shadow.animation.json");

    @Override
    public ResourceLocation getModelResource(VengefulShadowEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(VengefulShadowEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(VengefulShadowEntity animatable) { return ANIMATION; }
}
