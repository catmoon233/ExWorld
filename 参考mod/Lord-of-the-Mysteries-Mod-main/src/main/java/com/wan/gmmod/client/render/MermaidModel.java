package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.MermaidEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 美人鱼的 GeckoLib 模型资源定位。
 * <p>
 * 以下资源由用户自行提供：
 * <ul>
 *     <li>模型：{@code assets/guimi_mod/geo/mermaid.geo.json}</li>
 *     <li>贴图：{@code assets/guimi_mod/textures/entity/mermaid.png}</li>
 *     <li>动画：{@code assets/guimi_mod/animations/mermaid.animation.json}
 *     （需包含：animation.mermaid.move 行动（循环）、
 *     animation.mermaid.sing 技能·美人鱼的歌声（循环））</li>
 * </ul>
 */
public class MermaidModel extends GeoModel<MermaidEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/mermaid.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/mermaid.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/mermaid.animation.json");

    @Override
    public ResourceLocation getModelResource(MermaidEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MermaidEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MermaidEntity animatable) {
        return ANIMATION;
    }
}
