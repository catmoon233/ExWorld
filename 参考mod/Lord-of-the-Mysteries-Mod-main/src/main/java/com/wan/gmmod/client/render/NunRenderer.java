package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.NunEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 修女的 GeckoLib 实体渲染器。
 */
public class NunRenderer extends GeoEntityRenderer<NunEntity> {
    public NunRenderer(EntityRendererProvider.Context context) {
        super(context, new NunModel());
    }
}