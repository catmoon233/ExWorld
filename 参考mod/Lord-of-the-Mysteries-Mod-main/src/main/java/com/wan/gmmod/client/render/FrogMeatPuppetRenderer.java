package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.FrogMeatPuppetEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FrogMeatPuppetRenderer extends GeoEntityRenderer<FrogMeatPuppetEntity> {
    public FrogMeatPuppetRenderer(EntityRendererProvider.Context context) {
        super(context, new FrogMeatPuppetModel());
    }
}
