package com.wan.gmmod.client.render;

import com.wan.gmmod.client.SpiritVisionClient;
import com.wan.gmmod.content.entities.SpiritEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class SpiritRenderer extends HumanoidMobRenderer<SpiritEntity, HumanoidModel<SpiritEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("guimi_mod", "textures/entity/spirit.png");

    public SpiritRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritEntity entity) {
        return TEXTURE;
    }

    /** 灵体：仅当本地玩家开启灵视时才渲染 */
    @Override
    public boolean shouldRender(SpiritEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return SpiritVisionClient.isActive() && super.shouldRender(entity, frustum, camX, camY, camZ);
    }
}