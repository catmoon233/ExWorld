package com.wan.gmmod.client.render;

import com.wan.gmmod.common.item.DemonFormItem;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * 恶魔化全身模型渲染器：把骨骼映射到玩家身体对应部位。
 * <p>
 * 模型骨骼：bipedHead / bipedBody（有形状）+ armorRightArm / armorLeftArm /
 * armorRightLeg / armorLeftLeg（手臂与腿的形状直接放在 armor 系列骨骼上）
 * + armorRightBoot / armorLeftBoot + wing_l / wing_r / horn_l / horn_r3（翅膀与犄角）。
 * 覆盖 {@link #applyBoneVisibilityBySlot} 保持全部骨骼可见，使翅膀与犄角一并显示。
 */
public class DemonFormRenderer extends GeoArmorRenderer<DemonFormItem> {
    public DemonFormRenderer() {
        super(new DemonFormModel());
    }

    @Override
    public GeoBone getHeadBone(GeoModel<DemonFormItem> model) {
        return model.getBone("bipedHead").orElse(null);
    }

    @Override
    public GeoBone getBodyBone(GeoModel<DemonFormItem> model) {
        return model.getBone("bipedBody").orElse(null);
    }

    @Override
    public GeoBone getRightArmBone(GeoModel<DemonFormItem> model) {
        return model.getBone("armorRightArm").orElse(null);
    }

    @Override
    public GeoBone getLeftArmBone(GeoModel<DemonFormItem> model) {
        return model.getBone("armorLeftArm").orElse(null);
    }

    @Override
    public GeoBone getRightLegBone(GeoModel<DemonFormItem> model) {
        return model.getBone("armorRightLeg").orElse(null);
    }

    @Override
    public GeoBone getLeftLegBone(GeoModel<DemonFormItem> model) {
        return model.getBone("armorLeftLeg").orElse(null);
    }

    @Override
    public GeoBone getRightBootBone(GeoModel<DemonFormItem> model) {
        return model.getBone("armorRightBoot").orElse(null);
    }

    @Override
    public GeoBone getLeftBootBone(GeoModel<DemonFormItem> model) {
        return model.getBone("armorLeftBoot").orElse(null);
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        // 全身变身：全部骨骼保持可见（翅膀与犄角一并渲染）
    }
}
