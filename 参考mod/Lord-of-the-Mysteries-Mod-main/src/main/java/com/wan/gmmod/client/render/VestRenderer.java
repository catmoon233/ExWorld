package com.wan.gmmod.client.render;

import com.wan.gmmod.common.item.VestItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * 马甲盔甲 GeckoLib 渲染器类
 * <p>
 * 继承 {@link GeoArmorRenderer}，将 {@link VestModel} 的几何模型骨骼
 * 自动绑定到玩家身体模型（{@link net.minecraft.client.model.HumanoidModel}）。
 * <p>
 * GeckoLib 默认查找以下骨骼名并绑定到对应身体部位：
 * <ul>
 *   <li>{@code armorBody} → 玩家身体</li>
 *   <li>{@code armorRightArm} → 玩家右臂</li>
 *   <li>{@code armorLeftArm} → 玩家左臂</li>
 * </ul>
 * <p>
 * 如模型骨骼名不同，可重写 {@code getBodyBone}、{@code getRightArmBone} 等方法。
 *
 * @see GeoArmorRenderer
 * @see VestModel
 */
public class VestRenderer extends GeoArmorRenderer<VestItem> {
    /**
     * 构造马甲渲染器，传入自定义的 {@link VestModel}
     */
    public VestRenderer() {
        super(new VestModel());
    }
}
