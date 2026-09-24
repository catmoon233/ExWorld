package io.redspace.irons_artifice.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.irons_artifice.api.GunBones;
import io.redspace.irons_artifice.item.AttachmentMap;
import io.redspace.irons_artifice.registry.DataComponentRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/**
 * First-person bayonet swing driven by item-use ticks.
 * PORT-BLOCKED: KineticWeapon charge timings and hit feedback are not in 1.21.1.
 */
public class BayonetAnimations {
    private static final float RAISE_TICKS = 10.0F;

    public static void firstPersonUse(float ticksSinceKineticHitFeedback, PoseStack poseStack, float timeHeld, HumanoidArm arm, ItemStack itemStack) {
        AttachmentMap attachments = itemStack.get(DataComponentRegistry.ATTACHMENT);
        if (attachments == null || !attachments.attachments().containsKey(GunBones.SOCKET_BAYONET)) {
            return;
        }
        float raise = smoothstep(Mth.clamp(timeHeld / RAISE_TICKS, 0.0F, 1.0F));
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(invert * -0.5F * raise, -0.075F * raise, -0.75F * raise);
        poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F * raise));
        poseStack.mulPose(Axis.ZP.rotationDegrees(invert * -45.0F * raise));
        if (timeHeld > RAISE_TICKS) {
            float sway = Mth.sin(timeHeld * 19.0F * Mth.DEG_TO_RAD) * 0.01F;
            poseStack.translate(invert * sway, sway, 0.0F);
        }
        if (ticksSinceKineticHitFeedback > 0.0F) {
            float feedback = 0.4F * (1.0F - Mth.clamp(ticksSinceKineticHitFeedback / 10.0F, 0.0F, 1.0F));
            poseStack.translate(0.0F, -feedback, 0.0F);
        }
    }

    private static float smoothstep(float t) {
        return t * t * (3.0F - 2.0F * t);
    }
}
