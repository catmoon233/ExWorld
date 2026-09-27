package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.VestItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 马甲盔甲 GeckoLib 模型类
 * <p>
 * 为 {@link VestItem} 提供几何模型、纹理、动画的资源路径。
 * <p>
 * 资源文件位置：
 * <ul>
 *   <li>模型：{@code assets/guimi_mod/geo/vest.geo.json}</li>
 *   <li>纹理：{@code assets/guimi_mod/textures/models/armor/vest.png}</li>
 *   <li>动画：{@code assets/guimi_mod/animations/vest.animation.json}</li>
 * </ul>
 * <p>
 * 模型骨骼须包含 {@code armorBody}（身体），可选 {@code armorRightArm}、
 * {@code armorLeftArm}（手臂），以适配 GeckoLib {@code GeoArmorRenderer} 的骨骼绑定。
 *
 * @see VestRenderer
 */
public class VestModel extends GeoModel<VestItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/vest.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/models/armor/vest.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/vest.animation.json");

    /**
     * 获取几何模型资源路径
     *
     * @param animatable 被渲染的马甲物品
     * @return 几何模型 JSON 的资源路径
     */
    @Override
    public ResourceLocation getModelResource(VestItem animatable) {
        return MODEL;
    }

    /**
     * 获取纹理资源路径
     *
     * @param animatable 被渲染的马甲物品
     * @return 纹理 PNG 的资源路径
     */
    @Override
    public ResourceLocation getTextureResource(VestItem animatable) {
        return TEXTURE;
    }

    /**
     * 获取动画资源路径
     *
     * @param animatable 被渲染的马甲物品
     * @return 动画 JSON 的资源路径
     */
    @Override
    public ResourceLocation getAnimationResource(VestItem animatable) {
        return ANIMATION;
    }
}
