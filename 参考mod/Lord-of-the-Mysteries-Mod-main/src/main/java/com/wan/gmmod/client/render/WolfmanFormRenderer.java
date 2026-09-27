package com.wan.gmmod.client.render;

import com.wan.gmmod.common.item.WolfmanFormItem;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * 狼人化叠加模型渲染器：把骨骼映射到玩家身体对应部位。
 * <p>
 * 模型骨骼：body / head / arm_r（镜像后位于 -x=右手）/ arm_l / tail（局部变身，腿为空骨骼）。
 * 覆盖 {@link #applyBoneVisibilityBySlot} 保持全部骨骼可见，使尾巴等装饰骨骼一并显示。
 */
public class WolfmanFormRenderer extends GeoArmorRenderer<WolfmanFormItem> {
    public WolfmanFormRenderer() {
        super(new WolfmanFormModel());
    }

    @Override
    public GeoBone getHeadBone(GeoModel<WolfmanFormItem> model) {
        return model.getBone("head").orElse(null);
    }

    @Override
    public GeoBone getBodyBone(GeoModel<WolfmanFormItem> model) {
        return model.getBone("body").orElse(null);
    }

    @Override
    public GeoBone getRightArmBone(GeoModel<WolfmanFormItem> model) {
        return model.getBone("arm_r").orElse(null);
    }

    @Override
    public GeoBone getLeftArmBone(GeoModel<WolfmanFormItem> model) {
        return model.getBone("arm_l").orElse(null);
    }

    @Override
    public GeoBone getRightLegBone(GeoModel<WolfmanFormItem> model) {
        return model.getBone("leg_r").orElse(null);
    }

    @Override
    public GeoBone getLeftLegBone(GeoModel<WolfmanFormItem> model) {
        return model.getBone("leg_l").orElse(null);
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        // 局部变身：全部骨骼保持可见（尾巴等装饰骨骼一并渲染）
    }
}
