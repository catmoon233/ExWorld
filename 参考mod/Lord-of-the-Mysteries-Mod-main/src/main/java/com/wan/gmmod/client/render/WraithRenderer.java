package com.wan.gmmod.client.render;

import com.wan.gmmod.client.SpiritVisionClient;
import com.wan.gmmod.content.entities.WraithEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 怨灵的 GeckoLib 实体渲染器。
 * <p>
 * 怨灵属于灵体：仅当本地玩家开启灵视（{@code SpiritVisionClient.isActive()}）时才渲染，
 * 平时不可见。可见性通过覆盖 {@link #shouldRender} 实现，不影响服务端行为。
 */
public class WraithRenderer extends GeoEntityRenderer<WraithEntity> {
    public WraithRenderer(EntityRendererProvider.Context context) {
        super(context, new WraithModel());
    }

    @Override
    public boolean shouldRender(WraithEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return SpiritVisionClient.isActive() && super.shouldRender(entity, frustum, camX, camY, camZ);
    }
}
