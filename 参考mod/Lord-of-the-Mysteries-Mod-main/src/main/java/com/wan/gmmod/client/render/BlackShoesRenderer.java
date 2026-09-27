package com.wan.gmmod.client.render;

import com.wan.gmmod.common.item.BlackShoesItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * 黑皮鞋盔甲 GeckoLib 渲染器类
 * <p>
 * 继承 {@link GeoArmorRenderer}，将 {@link BlackShoesModel} 的几何模型骨骼
 * 自动绑定到玩家身体模型（{@link net.minecraft.client.model.HumanoidModel}）。
 * <p>
 * 重写靴子与腿部骨骼绑定方法，将 GeckoLib 默认骨骼名映射到自定义骨骼名：
 * <ul>
 *   <li>{@code armorRightFoot} → 玩家右腿</li>
 *   <li>{@code armorLeftFoot} → 玩家左腿</li>
 * </ul>
 * <p>
 * 注意：GeckoLib 的 {@code applyBaseTransformations} 中，靴子骨骼的位置/旋转匹配
 * 嵌套在「腿部骨骼非空」判断内，因此必须<b>同时</b>重写腿部骨骼方法
 * （{@code getRightLegBone}/{@code getLeftLegBone}），否则靴子不会跟随腿部运动。
 * 腿部与靴子均指向同一自定义骨骼，可保证位置匹配触发且在鞋子槽（FEET）正常可见。
 *
 * @see GeoArmorRenderer
 * @see BlackShoesModel
 */
public class BlackShoesRenderer extends GeoArmorRenderer<BlackShoesItem> {
    /**
     * 构造黑皮鞋渲染器，传入自定义的 {@link BlackShoesModel}
     */
    public BlackShoesRenderer() {
        super(new BlackShoesModel());
    }

    /**
     * 重写右靴子骨骼绑定，映射到自定义骨骼名 {@code armorRightFoot}
     *
     * @param model 黑皮鞋 GeckoLib 模型
     * @return 右靴子骨骼对象
     */
    @Override
    public GeoBone getRightBootBone(GeoModel<BlackShoesItem> model) {
        return model.getBone("armorRightFoot").orElse(null);
    }

    /**
     * 重写左靴子骨骼绑定，映射到自定义骨骼名 {@code armorLeftFoot}
     *
     * @param model 黑皮鞋 GeckoLib 模型
     * @return 左靴子骨骼对象
     */
    @Override
    public GeoBone getLeftBootBone(GeoModel<BlackShoesItem> model) {
        return model.getBone("armorLeftFoot").orElse(null);
    }

    /**
     * 重写右腿骨骼绑定，映射到自定义骨骼名 {@code armorRightFoot}
     * <p>
     * 触发 GeckoLib 对腿部（及嵌套的靴子）骨骼的位置/旋转匹配，
     * 使黑皮鞋跟随玩家右腿运动。
     *
     * @param model 黑皮鞋 GeckoLib 模型
     * @return 右腿骨骼对象
     */
    @Override
    public GeoBone getRightLegBone(GeoModel<BlackShoesItem> model) {
        return model.getBone("armorRightFoot").orElse(null);
    }

    /**
     * 重写左腿骨骼绑定，映射到自定义骨骼名 {@code armorLeftFoot}
     * <p>
     * 触发 GeckoLib 对腿部（及嵌套的靴子）骨骼的位置/旋转匹配，
     * 使黑皮鞋跟随玩家左腿运动。
     *
     * @param model 黑皮鞋 GeckoLib 模型
     * @return 左腿骨骼对象
     */
    @Override
    public GeoBone getLeftLegBone(GeoModel<BlackShoesItem> model) {
        return model.getBone("armorLeftFoot").orElse(null);
    }
}
