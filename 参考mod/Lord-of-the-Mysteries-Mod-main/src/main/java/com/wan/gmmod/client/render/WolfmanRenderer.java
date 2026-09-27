package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.WolfmanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 狼人的 GeckoLib 实体渲染器。
 */
public class WolfmanRenderer extends GeoEntityRenderer<WolfmanEntity> {
    public WolfmanRenderer(EntityRendererProvider.Context context) {
        super(context, new WolfmanModel());
    }
}
