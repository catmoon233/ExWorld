package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.ThousandFacedHunterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ThousandFacedHunterRenderer extends GeoEntityRenderer<ThousandFacedHunterEntity> {
    public ThousandFacedHunterRenderer(EntityRendererProvider.Context context) {
        super(context, new ThousandFacedHunterModel());
    }
}