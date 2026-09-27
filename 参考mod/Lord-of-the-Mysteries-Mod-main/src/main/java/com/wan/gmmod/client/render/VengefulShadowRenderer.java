package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.VengefulShadowEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VengefulShadowRenderer extends GeoEntityRenderer<VengefulShadowEntity> {
    public VengefulShadowRenderer(EntityRendererProvider.Context context) {
        super(context, new VengefulShadowModel());
    }
}
