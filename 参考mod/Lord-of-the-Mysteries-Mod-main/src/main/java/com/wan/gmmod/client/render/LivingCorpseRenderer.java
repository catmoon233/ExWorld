package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.LivingCorpseEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LivingCorpseRenderer extends GeoEntityRenderer<LivingCorpseEntity> {
    public LivingCorpseRenderer(EntityRendererProvider.Context context) {
        super(context, new LivingCorpseModel());
    }
}
