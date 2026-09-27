package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.HornachisGoatEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HornachisGoatRenderer extends GeoEntityRenderer<HornachisGoatEntity> {
    public HornachisGoatRenderer(EntityRendererProvider.Context context) {
        super(context, new HornachisGoatModel());
    }
}