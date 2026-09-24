package io.redspace.irons_artifice.client.gun;

import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.ReloadState;
import io.redspace.irons_artifice.registry.DataComponentRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public final class GunArmPoses {
    private static final float QUARTER_PI = Mth.PI * 0.25f;
    public static final EnumProxy<HumanoidModel.ArmPose> PISTOL = new EnumProxy<>(
            HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) GunArmPoses::applyPistolPose
    );
    public static final EnumProxy<HumanoidModel.ArmPose> RIFLE = new EnumProxy<>(
            HumanoidModel.ArmPose.class, true, (IArmPoseTransformer) GunArmPoses::applyRiflePose
    );

    private static void applyPistolPose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        boolean holdingInRightArm = arm == HumanoidArm.RIGHT;
        ItemStack stack = stackInArm(entity, arm);
        float partialTick = partialTick();
        ReloadState reloadState = ReloadState.get(stack);
        if (reloadState != null && !reloadState.isFinished()) {
            animateCrossbowCharge(model.rightArm, model.leftArm, 1f, reloadState.percent(partialTick), holdingInRightArm);
        } else {
            ModelPart armModel = holdingInRightArm ? model.rightArm : model.leftArm;
            armModel.yRot = model.head.yRot;
            armModel.xRot = -1.5F + model.head.xRot;
            counteractCrouch(armModel, entity);
            handleUseAnimation(entity, arm, stack, armModel, holdingInRightArm);
        }
    }

    private static void handleUseAnimation(LivingEntity entity, HumanoidArm arm, ItemStack stack, ModelPart armModel, boolean holdingInRightArm) {
        InteractionHand hand = handForArm(entity, arm);
        if (!entity.isUsingItem() || entity.getUsedItemHand() != hand) {
            return;
        }
        if (GunItem.isChargingBayonet(entity)) {
            handleBayonetPose(armModel, holdingInRightArm);
        } else if (stack.has(DataComponentRegistry.GUN_SPYGLASS)) {
            handleScopingPose(armModel, holdingInRightArm);
        }
    }

    private static void handleBayonetPose(ModelPart arm, boolean isRightArm) {
        arm.x += 4f * (isRightArm ? 1 : -1);
        arm.z += -4f;
    }

    private static void handleScopingPose(ModelPart arm, boolean isRightArm) {
        arm.x += 3f * (isRightArm ? 1 : -1);
        arm.z += -1;
        arm.y += -1;
    }

    private static void applyRiflePose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        boolean holdingInRightArm = arm == HumanoidArm.RIGHT;
        ItemStack stack = stackInArm(entity, arm);
        float partialTick = partialTick();
        ModelPart shootingArm = holdingInRightArm ? model.rightArm : model.leftArm;
        ModelPart supportArm = holdingInRightArm ? model.leftArm : model.rightArm;
        ReloadState reloadState = ReloadState.get(stack);
        if (reloadState != null && !reloadState.isFinished()) {
            animateCrossbowCharge(model.rightArm, model.leftArm, 1f, reloadState.percent(partialTick), holdingInRightArm);
        } else {
            ModelPart head = model.head;
            shootingArm.z += 1;
            shootingArm.yRot = head.yRot;
            shootingArm.xRot = (-(float) Math.PI / 2F) + head.xRot + 0.1F;
            supportArm.yRot = (holdingInRightArm ? 0.8F : -0.8F) + head.yRot;
            supportArm.xRot = -1.5F + head.xRot;
            supportArm.z -= 3;
            float f = -Math.min(head.yRot, QUARTER_PI) / QUARTER_PI;
            if (head.yRot > 0) {
                supportArm.x += f * 4;
                supportArm.z += f * 2;
                shootingArm.z -= f * 2;
            } else {
                supportArm.z += f * 4;
            }
            counteractCrouch(supportArm, entity);
            counteractCrouch(shootingArm, entity);
            handleUseAnimation(entity, arm, stack, shootingArm, holdingInRightArm);
        }
        supportArm.y += 1;
        shootingArm.y += 2;
    }

    private static void counteractCrouch(ModelPart arm, LivingEntity entity) {
        if (entity.isCrouching()) {
            arm.xRot -= 0.4f;
        }
    }

    public static void animateCrossbowCharge(ModelPart rightArm, ModelPart leftArm, float maxCrossbowChargeDuration, float ticksUsingItem, boolean holdingInRightArm) {
        ModelPart holdingArm = holdingInRightArm ? rightArm : leftArm;
        ModelPart pullingArm = holdingInRightArm ? leftArm : rightArm;
        holdingArm.yRot = holdingInRightArm ? -0.8F : 0.8F;
        holdingArm.xRot = -0.97079635F;
        pullingArm.xRot = holdingArm.xRot;
        float useTicks = Mth.clamp(ticksUsingItem, 0.0F, maxCrossbowChargeDuration);
        float lerpAlpha = useTicks / maxCrossbowChargeDuration;
        lerpAlpha = Mth.sin(lerpAlpha * Mth.TWO_PI) * 0.5f + 0.5f;
        pullingArm.yRot = Mth.lerp(lerpAlpha, 0.4F, 0.85F) * (holdingInRightArm ? 1 : -1);
        pullingArm.xRot = Mth.lerp(lerpAlpha, pullingArm.xRot, (-(float) Math.PI / 2F));
    }

    private static ItemStack stackInArm(LivingEntity entity, HumanoidArm arm) {
        return entity.getItemInHand(handForArm(entity, arm));
    }

    private static InteractionHand handForArm(LivingEntity entity, HumanoidArm arm) {
        return entity.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    private static float partialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }
}
