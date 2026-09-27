package net.exmo.lotm.mixin.client;

import net.exmo.lotm.client.NightSkyLight;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Washes the sky dome toward the same faded red as the fog, so the horizon has no mask edge. */
@Mixin(ClientLevel.class)
public abstract class NightSkyColorMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void lotm$tintSky(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL) return;
        cir.setReturnValue(NightSkyLight.tintDome(cir.getReturnValue(), level.getSkyDarken(partialTick), level.getRainLevel(partialTick)));
    }
}
