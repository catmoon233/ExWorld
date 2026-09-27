package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.RainBirdEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RainBirdRenderer extends GeoEntityRenderer<RainBirdEntity> {
    public RainBirdRenderer(EntityRendererProvider.Context context) {
        super(context, new RainBirdModel());
    }
}
