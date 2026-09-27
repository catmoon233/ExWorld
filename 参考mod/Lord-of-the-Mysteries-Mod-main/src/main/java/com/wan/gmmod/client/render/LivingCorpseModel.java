package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.LivingCorpseEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class LivingCorpseModel extends GeoModel<LivingCorpseEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/living_corpse.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/living_corpse.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/living_corpse.animation.json");

    @Override
    public ResourceLocation getModelResource(LivingCorpseEntity animatable) { return MODEL; }
    @Override
    public ResourceLocation getTextureResource(LivingCorpseEntity animatable) { return TEXTURE; }
    @Override
    public ResourceLocation getAnimationResource(LivingCorpseEntity animatable) { return ANIMATION; }
}
