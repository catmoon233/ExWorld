package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.EvilBlackCatEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class EvilBlackCatRenderer extends GeoEntityRenderer<EvilBlackCatEntity> {
    public EvilBlackCatRenderer(EntityRendererProvider.Context context) {
        super(context, new EvilBlackCatModel());
    }
}
