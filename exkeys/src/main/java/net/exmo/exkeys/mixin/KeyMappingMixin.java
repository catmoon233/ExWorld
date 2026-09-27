package net.exmo.exkeys.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.exmo.exkeys.client.ExKeysClient;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {
    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void exkeys$blockDown(CallbackInfoReturnable<Boolean> cir) {
        if (ExKeysClient.blocks((KeyMapping) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void exkeys$blockConsume(CallbackInfoReturnable<Boolean> cir) {
        KeyMapping self = (KeyMapping) (Object) this;
        if (!ExKeysClient.blocks(self)) return;
        ExKeysClient.silence(self);
        cir.setReturnValue(false);
    }

    @Inject(method = "setDown", at = @At("HEAD"), cancellable = true)
    private void exkeys$blockSetDown(boolean down, CallbackInfo ci) {
        if (down && ExKeysClient.blocks((KeyMapping) (Object) this)) ci.cancel();
    }

    @Inject(method = "click", at = @At("TAIL"))
    private static void exkeys$afterClick(InputConstants.Key key, CallbackInfo ci) {
        ExKeysClient.releaseBlocked(key);
    }
}
