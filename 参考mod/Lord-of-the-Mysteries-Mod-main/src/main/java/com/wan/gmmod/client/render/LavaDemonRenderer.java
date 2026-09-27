package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.LavaDemonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LavaDemonRenderer extends GeoEntityRenderer<LavaDemonEntity> {
    public LavaDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new LavaDemonModel());
    }
}