package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.AdultUnicornEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** 成年独角兽的 GeckoLib 模型资源定位。 */
public class AdultUnicornModel extends GeoModel<AdultUnicornEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/adult_unicorn.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/adult_unicorn.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/adult_unicorn.animation.json");

    @Override
    public ResourceLocation getModelResource(AdultUnicornEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AdultUnicornEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AdultUnicornEntity animatable) {
        return ANIMATION;
    }
}
