package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.HumanSkinShadowEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HumanSkinShadowRenderer extends GeoEntityRenderer<HumanSkinShadowEntity> {
    public HumanSkinShadowRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanSkinShadowModel());
    }
}