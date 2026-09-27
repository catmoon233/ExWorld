package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.BlackScaleSharkEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BlackScaleSharkRenderer extends GeoEntityRenderer<BlackScaleSharkEntity> {
    public BlackScaleSharkRenderer(EntityRendererProvider.Context context) {
        super(context, new BlackScaleSharkModel());
    }
}
