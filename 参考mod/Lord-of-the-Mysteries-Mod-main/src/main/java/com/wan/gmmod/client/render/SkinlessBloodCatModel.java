package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.SkinlessBloodCatEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** 无皮血猫的 GeckoLib 模型资源定位。 */
public class SkinlessBloodCatModel extends GeoModel<SkinlessBloodCatEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/skinless_blood_cat.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/skinless_blood_cat.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/skinless_blood_cat.animation.json");

    @Override
    public ResourceLocation getModelResource(SkinlessBloodCatEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SkinlessBloodCatEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SkinlessBloodCatEntity animatable) {
        return ANIMATION;
    }
}
