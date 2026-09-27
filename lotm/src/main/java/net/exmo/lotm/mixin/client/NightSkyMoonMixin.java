package net.exmo.lotm.mixin.client;

import net.exmo.lotm.client.NightSkyLight;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the faded moon shader color immediately before the moon quad is drawn. */
@Mixin(LevelRenderer.class)
public abstract class NightSkyMoonMixin {
    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getMoonPhase()I"))
    private void lotm$tintMoon(Matrix4f modelView, Matrix4f projection, float partialTick, Camera camera,
                               boolean foggy, Runnable fogSetup, CallbackInfo ci) {
        NightSkyLight.tintMoon(partialTick);
    }
}
