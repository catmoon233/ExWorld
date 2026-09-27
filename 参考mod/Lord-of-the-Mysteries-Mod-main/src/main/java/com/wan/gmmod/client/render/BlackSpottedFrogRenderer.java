package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.BlackSpottedFrogEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BlackSpottedFrogRenderer extends GeoEntityRenderer<BlackSpottedFrogEntity> {
    public BlackSpottedFrogRenderer(EntityRendererProvider.Context context) {
        super(context, new BlackSpottedFrogModel());
    }
}
