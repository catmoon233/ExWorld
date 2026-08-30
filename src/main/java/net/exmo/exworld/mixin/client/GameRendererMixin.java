package net.exmo.exworld.mixin.client;

import net.exmo.exworld.client.camera.AdvancedCameraDirector;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void exworld$cinematicFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> callback) {
        if (AdvancedCameraDirector.active()) callback.setReturnValue((double) AdvancedCameraDirector.fov(partialTick));
    }
}
