package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.RottenShepherdEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RottenShepherdRenderer extends GeoEntityRenderer<RottenShepherdEntity> {
    public RottenShepherdRenderer(EntityRendererProvider.Context context) {
        super(context, new RottenShepherdModel());
    }
}
