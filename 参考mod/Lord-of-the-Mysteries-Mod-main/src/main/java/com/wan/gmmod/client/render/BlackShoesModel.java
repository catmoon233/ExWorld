package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.BlackShoesItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 黑皮鞋盔甲 GeckoLib 模型类
 * <p>
 * 为 {@link BlackShoesItem} 提供几何模型、纹理、动画的资源路径。
 * <p>
 * 资源文件位置：
 * <ul>
 *   <li>模型：{@code assets/guimi_mod/geo/black_shoes.geo.json}</li>
 *   <li>纹理：{@code assets/guimi_mod/textures/models/armor/black_shoes.png}</li>
 *   <li>动画：{@code assets/guimi_mod/animations/black_shoes.animation.json}</li>
 * </ul>
 * <p>
 * 模型骨骼须包含 {@code armorRightLeg}、{@code armorLeftLeg}（腿部），
 * 以适配 GeckoLib {@code GeoArmorRenderer} 的骨骼绑定。
 *
 * @see BlackShoesRenderer
 */
public class BlackShoesModel extends GeoModel<BlackShoesItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/black_shoes.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/models/armor/black_shoes.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/black_shoes.animation.json");

    /**
     * 获取几何模型资源路径
     *
     * @param animatable 被渲染的黑皮鞋物品
     * @return 几何模型 JSON 的资源路径
     */
    @Override
    public ResourceLocation getModelResource(BlackShoesItem animatable) {
        return MODEL;
    }

    /**
     * 获取纹理资源路径
     *
     * @param animatable 被渲染的黑皮鞋物品
     * @return 纹理 PNG 的资源路径
     */
    @Override
    public ResourceLocation getTextureResource(BlackShoesItem animatable) {
        return TEXTURE;
    }

    /**
     * 获取动画资源路径
     *
     * @param animatable 被渲染的黑皮鞋物品
     * @return 动画 JSON 的资源路径
     */
    @Override
    public ResourceLocation getAnimationResource(BlackShoesItem animatable) {
        return ANIMATION;
    }
}
