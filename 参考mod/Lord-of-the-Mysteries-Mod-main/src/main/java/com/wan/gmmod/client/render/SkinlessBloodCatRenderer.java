package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.SkinlessBloodCatEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 无皮血猫的 GeckoLib 实体渲染器。 */
public class SkinlessBloodCatRenderer extends GeoEntityRenderer<SkinlessBloodCatEntity> {
    public SkinlessBloodCatRenderer(EntityRendererProvider.Context context) {
        super(context, new SkinlessBloodCatModel());
    }
}
