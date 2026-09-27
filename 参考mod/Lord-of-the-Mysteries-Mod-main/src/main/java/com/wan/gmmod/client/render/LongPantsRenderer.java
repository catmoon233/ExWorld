package com.wan.gmmod.client.render;

import com.wan.gmmod.common.item.LongPantsItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * 黑色长裤盔甲 GeckoLib 渲染器类
 * <p>
 * 继承 {@link GeoArmorRenderer}，将 {@link LongPantsModel} 的几何模型骨骼
 * 自动绑定到玩家身体模型（{@link net.minecraft.client.model.HumanoidModel}）。
 * <p>
 * GeckoLib 默认查找以下骨骼名并绑定到对应身体部位：
 * <ul>
 *   <li>{@code armorRightLeg} → 玩家右腿</li>
 *   <li>{@code armorLeftLeg} → 玩家左腿</li>
 * </ul>
 * <p>
 * 如模型骨骼名不同，可重写 {@code getRightLegBone}、{@code getLeftLegBone} 等方法。
 *
 * @see GeoArmorRenderer
 * @see LongPantsModel
 */
public class LongPantsRenderer extends GeoArmorRenderer<LongPantsItem> {
    /**
     * 构造黑色长裤渲染器，传入自定义的 {@link LongPantsModel}
     */
    public LongPantsRenderer() {
        super(new LongPantsModel());
    }
}
