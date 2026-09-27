package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.AdultPegasusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 成年飞马的 GeckoLib 实体渲染器。 */
public class AdultPegasusRenderer extends GeoEntityRenderer<AdultPegasusEntity> {
    public AdultPegasusRenderer(EntityRendererProvider.Context context) {
        super(context, new AdultPegasusModel());
    }
}
