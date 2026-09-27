package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.DeathRavenEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DeathRavenRenderer extends GeoEntityRenderer<DeathRavenEntity> {
    public DeathRavenRenderer(EntityRendererProvider.Context context) {
        super(context, new DeathRavenModel());
    }
}
