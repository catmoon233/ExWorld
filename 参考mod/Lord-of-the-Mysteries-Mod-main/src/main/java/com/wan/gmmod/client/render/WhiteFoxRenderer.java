package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.WhiteFoxEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WhiteFoxRenderer extends GeoEntityRenderer<WhiteFoxEntity> {
    public WhiteFoxRenderer(EntityRendererProvider.Context context) {
        super(context, new WhiteFoxModel());
    }
}