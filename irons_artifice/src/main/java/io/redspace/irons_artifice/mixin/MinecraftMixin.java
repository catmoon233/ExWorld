package io.redspace.irons_artifice.mixin;

import io.redspace.irons_artifice.item.GunItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void irons_artifice$suppressGunHeldAttack(boolean down, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().getItem() instanceof GunItem) {
            ci.cancel();
        }
    }
}
