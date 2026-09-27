package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.NightmareEyeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 噩梦邪眼的 GeckoLib 实体渲染器（单渲染器承载双形态）。
 * <p>
 * 模型 / 纹理 / 动画的形态切换逻辑全部在 {@link NightmareEyeModel} 中按实体状态完成。
 */
public class NightmareEyeRenderer extends GeoEntityRenderer<NightmareEyeEntity> {
    public NightmareEyeRenderer(EntityRendererProvider.Context context) {
        super(context, new NightmareEyeModel());
    }
}
