package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.NightmareShadowEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class NightmareShadowRenderer extends GeoEntityRenderer<NightmareShadowEntity> {
    public NightmareShadowRenderer(EntityRendererProvider.Context context) {
        super(context, new NightmareShadowModel());
    }
}
