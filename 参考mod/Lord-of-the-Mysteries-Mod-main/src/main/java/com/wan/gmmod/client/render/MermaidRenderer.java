package com.wan.gmmod.client.render;

import com.wan.gmmod.content.entities.MermaidEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 美人鱼的 GeckoLib 实体渲染器。
 * <p>
 * 美人鱼不是灵体，普通玩家可以直接看见。
 */
public class MermaidRenderer extends GeoEntityRenderer<MermaidEntity> {
    public MermaidRenderer(EntityRendererProvider.Context context) {
        super(context, new MermaidModel());
    }
}
