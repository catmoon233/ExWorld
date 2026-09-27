package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.DawnRoosterEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** 黎明雄鸡的 GeckoLib 模型资源定位。 */
public class DawnRoosterModel extends GeoModel<DawnRoosterEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/dawn_rooster.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/dawn_rooster.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/dawn_rooster.animation.json");

    @Override
    public ResourceLocation getModelResource(DawnRoosterEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DawnRoosterEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DawnRoosterEntity animatable) {
        return ANIMATION;
    }
}
