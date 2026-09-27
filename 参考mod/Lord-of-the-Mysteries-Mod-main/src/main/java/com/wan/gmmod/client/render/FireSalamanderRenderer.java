package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.FireSalamanderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FireSalamanderRenderer extends GeoEntityRenderer<FireSalamanderEntity> {
    public FireSalamanderRenderer(EntityRendererProvider.Context context) {
        super(context, new FireSalamanderModel());
    }
}
