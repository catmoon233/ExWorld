package io.redspace.irons_artifice.mixin;

import io.redspace.irons_artifice.client.gun.GunArmPoses;
import io.redspace.irons_artifice.gun.ArmPoseKind;
import io.redspace.irons_artifice.item.GunItem;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrownedModel.class)
public class DrownedModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/monster/Zombie;FFFFF)V", at = @At("TAIL"))
    private void irons_artifice$drownedGunAnimation(Zombie zombie, float limbSwing, float limbSwingAmount, float ageInTicks, float yaw, float pitch, CallbackInfo ci) {
        if (!(zombie instanceof Drowned drowned)) {
            return;
        }
        apply(drowned, drowned.getMainHandItem(), drowned.getMainArm());
        HumanoidArm offArm = drowned.getMainArm() == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        apply(drowned, drowned.getOffhandItem(), offArm);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void apply(Drowned drowned, ItemStack stack, HumanoidArm arm) {
        if (stack.getItem() instanceof GunItem gunItem) {
            var pose = gunItem.getGun().armPoseKind() == ArmPoseKind.PISTOL ? GunArmPoses.PISTOL.getValue() : GunArmPoses.RIFLE.getValue();
            pose.applyTransform((DrownedModel) (Object) this, drowned, arm);
        }
    }
}
