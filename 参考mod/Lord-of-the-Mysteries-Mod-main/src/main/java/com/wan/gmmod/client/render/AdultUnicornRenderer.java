package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.AdultUnicornEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 成年独角兽的 GeckoLib 实体渲染器。 */
public class AdultUnicornRenderer extends GeoEntityRenderer<AdultUnicornEntity> {
    public AdultUnicornRenderer(EntityRendererProvider.Context context) {
        super(context, new AdultUnicornModel());
    }
}
