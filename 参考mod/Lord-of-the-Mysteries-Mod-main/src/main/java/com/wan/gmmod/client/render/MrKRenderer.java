package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.MrKEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MrKRenderer extends GeoEntityRenderer<MrKEntity> {
    public MrKRenderer(EntityRendererProvider.Context context) {
        super(context, new MrKModel());
    }
}