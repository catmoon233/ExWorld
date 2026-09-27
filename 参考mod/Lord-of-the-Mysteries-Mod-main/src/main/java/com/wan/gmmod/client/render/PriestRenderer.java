package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.PriestEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 神父的 GeckoLib 实体渲染器。
 */
public class PriestRenderer extends GeoEntityRenderer<PriestEntity> {
    public PriestRenderer(EntityRendererProvider.Context context) {
        super(context, new PriestModel());
    }
}