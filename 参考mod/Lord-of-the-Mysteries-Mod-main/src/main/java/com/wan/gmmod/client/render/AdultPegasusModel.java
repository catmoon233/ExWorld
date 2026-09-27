package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.AdultPegasusEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** 成年飞马的 GeckoLib 模型资源定位。 */
public class AdultPegasusModel extends GeoModel<AdultPegasusEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/adult_pegasus.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/adult_pegasus.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/adult_pegasus.animation.json");

    @Override
    public ResourceLocation getModelResource(AdultPegasusEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AdultPegasusEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AdultPegasusEntity animatable) {
        return ANIMATION;
    }
}
