package net.exmo.exworld.mixin.compat;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents VoiceCastAddon's original direct-spell controller from bypassing ExWorld cards. */
@Mixin(targets = "com.yelle233.voicecastaddon.client.VoiceInputController")
public abstract class VoiceCastAddonMixin {
    @Inject(method = "onClientTick", at = @At("HEAD"), cancellable = true)
    private static void exworld$disableDirectCasting(ClientTickEvent.Post event, CallbackInfo callback) {
        callback.cancel();
    }
}
