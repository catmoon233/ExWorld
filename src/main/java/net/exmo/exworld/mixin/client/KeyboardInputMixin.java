package net.exmo.exworld.mixin.client;

import net.exmo.exworld.client.perspective.CameraProfile;
import net.exmo.exworld.client.perspective.DungeonPerspective;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.util.Mth;
import net.exmo.exworld.client.battle.BattleClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps WASD aligned with the fixed dungeon camera instead of the independently rotating player. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void exworld$cameraRelativeMovement(boolean slowDown, float slowDownFactor, CallbackInfo callback) {
        if (BattleClient.active()) {
            if (Minecraft.getInstance().screen == null) {
                BattleClient.panCamera((this.up ? 1.0F : 0.0F) - (this.down ? 1.0F : 0.0F),
                        (this.right ? 1.0F : 0.0F) - (this.left ? 1.0F : 0.0F));
            }
            this.forwardImpulse = 0; this.leftImpulse = 0;
            this.up = this.down = this.left = this.right = this.jumping = this.shiftKeyDown = false;
            return;
        }
        if (!DungeonPerspective.active()) return;
        CameraProfile profile = DungeonPerspective.profile();
        if (!profile.cameraRelativeMovement() || Minecraft.getInstance().player == null) return;

        // Vanilla third-person camera follows the player's live yaw; keep this seam for future detached camera modes.
        float delta = (DungeonPerspective.cameraYaw() - Minecraft.getInstance().player.getYRot()) * Mth.DEG_TO_RAD;
        float cos = Mth.cos(delta);
        float sin = Mth.sin(delta);
        float originalForward = this.forwardImpulse;
        float originalLeft = this.leftImpulse;
        this.forwardImpulse = originalForward * cos + originalLeft * sin;
        this.leftImpulse = -originalForward * sin + originalLeft * cos;
    }
}
