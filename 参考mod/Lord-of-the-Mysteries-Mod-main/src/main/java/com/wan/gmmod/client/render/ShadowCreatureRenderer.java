package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.ShadowCreatureEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 阴影生物的 GeckoLib 实体渲染器。
 */
public class ShadowCreatureRenderer extends GeoEntityRenderer<ShadowCreatureEntity> {
    public ShadowCreatureRenderer(EntityRendererProvider.Context context) {
        super(context, new ShadowCreatureModel());
    }
}