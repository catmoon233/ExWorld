package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.AbyssDemonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AbyssDemonRenderer extends GeoEntityRenderer<AbyssDemonEntity> {
    public AbyssDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssDemonModel());
    }
}