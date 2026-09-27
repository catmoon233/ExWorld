package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.EvilPantherEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class EvilPantherRenderer extends GeoEntityRenderer<EvilPantherEntity> {
    public EvilPantherRenderer(EntityRendererProvider.Context context) {
        super(context, new EvilPantherModel());
    }
}