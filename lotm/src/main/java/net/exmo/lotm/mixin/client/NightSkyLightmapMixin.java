package net.exmo.lotm.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.exmo.lotm.client.NightSkyLight;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Tints the sky-light vector before it is added to block light, matching Enhanced Celestials. */
@Mixin(LightTexture.class)
public abstract class NightSkyLightmapMixin {
    @ModifyExpressionValue(method = "updateLightTexture", at = @At(
            value = "INVOKE",
            target = "Lorg/joml/Vector3f;lerp(Lorg/joml/Vector3fc;F)Lorg/joml/Vector3f;",
            ordinal = 0))
    private Vector3f lotm$tintSkyLight(Vector3f skyVector) {
        NightSkyLight.tintCapturedSkyLight(skyVector);
        return skyVector;
    }
}
