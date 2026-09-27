package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.OneEyedBullEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OneEyedBullRenderer extends GeoEntityRenderer<OneEyedBullEntity> {
    public OneEyedBullRenderer(EntityRendererProvider.Context context) {
        super(context, new OneEyedBullModel());
    }
}
