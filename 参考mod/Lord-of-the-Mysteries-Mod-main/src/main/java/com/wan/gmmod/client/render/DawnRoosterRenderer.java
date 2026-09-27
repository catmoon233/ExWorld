package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.DawnRoosterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 黎明雄鸡的 GeckoLib 实体渲染器。 */
public class DawnRoosterRenderer extends GeoEntityRenderer<DawnRoosterEntity> {
    public DawnRoosterRenderer(EntityRendererProvider.Context context) {
        super(context, new DawnRoosterModel());
    }
}
