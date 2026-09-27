package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.SilverWarBearEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 银白战熊的 GeckoLib 实体渲染器。 */
public class SilverWarBearRenderer extends GeoEntityRenderer<SilverWarBearEntity> {
    public SilverWarBearRenderer(EntityRendererProvider.Context context) {
        super(context, new SilverWarBearModel());
    }
}
