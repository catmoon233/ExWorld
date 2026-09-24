package net.exmo.exworld.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Non-creative players cannot open the Tab player list. */
@Mixin(Gui.class)
public abstract class GuiTabListMixin {
    @Inject(method = "renderTabList", at = @At("HEAD"), cancellable = true)
    private void exworld$hidePlayerList(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo callback) {
        var player = Minecraft.getInstance().player;
        if (player != null && !player.isCreative()) callback.cancel();
    }
}
