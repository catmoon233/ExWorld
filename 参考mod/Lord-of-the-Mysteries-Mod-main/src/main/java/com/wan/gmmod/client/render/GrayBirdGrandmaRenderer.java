package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.GrayBirdGrandmaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GrayBirdGrandmaRenderer extends GeoEntityRenderer<GrayBirdGrandmaEntity> {
    public GrayBirdGrandmaRenderer(EntityRendererProvider.Context context) {
        super(context, new GrayBirdGrandmaModel());
    }
}
