package io.redspace.irons_artifice.client.entity.illificer;

import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.irons_artifice.client.gun.GunArmPoses;
import com.mojang.math.Axis;
import io.redspace.irons_artifice.entity.Illificer;
import io.redspace.irons_artifice.gun.ArmPoseKind;
import io.redspace.irons_artifice.item.FireDelayState;
import io.redspace.irons_artifice.item.GunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/** Illager body with the same idle and aiming arm angles the 26.1 render state used. */
public class IllificerModel extends IllagerModel<Illificer> {
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public IllificerModel(ModelPart root) {
        super(root);
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.getHat().visible = true;
    }

    @Override
    public void setupAnim(Illificer entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        ItemStack weapon = entity.getMainHandItem();
        if (!(weapon.getItem() instanceof GunItem gun)) {
            return;
        }
        boolean aiming = entity.isAggressive()
                || FireDelayState.isActive(entity, weapon)
                || GunItem.isReloading(weapon);
        if (aiming) {
            HumanoidModel.ArmPose pose = gun.getGun().armPoseKind() == ArmPoseKind.PISTOL
                    ? GunArmPoses.PISTOL.getValue()
                    : GunArmPoses.RIFLE.getValue();
            pose.applyTransform(new HumanoidModel<>(this.root()), entity, HumanoidArm.RIGHT);
            return;
        }
        ModelPart arm = rightArm;
        arm.xRot *= 0.25f;
        arm.xRot -= Mth.PI / 6f;
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        super.translateToHand(arm, poseStack);
        if (arm == HumanoidArm.RIGHT) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.translate(0, -1, 0);
        }
    }
}
