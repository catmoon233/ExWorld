package net.exmo.exworld.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.exmo.exworld.Config;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Decryption mode keeps player identities off the world view, including the below-name score line. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerNameTagMixin {
    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    private void exworld$hidePlayerNameTag(AbstractClientPlayer player, Component name, PoseStack pose,
                                            MultiBufferSource buffer, int light, float partialTick,
                                            CallbackInfo callback) {
        if (Config.decryptionMode) callback.cancel();
    }
}
