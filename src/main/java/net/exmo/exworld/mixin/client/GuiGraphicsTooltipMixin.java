package net.exmo.exworld.mixin.client;

import net.exmo.exworld.client.tooltip.TooltipCapture;
import net.exmo.exworld.client.tooltip.TooltipRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(value = GuiGraphics.class, priority = 1100)
public abstract class GuiGraphicsTooltipMixin {
    @Shadow(remap = false)
    private ItemStack tooltipStack;

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
    private void exworld$captureItemTooltip(Font font, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (stack != null && !stack.isEmpty()) TooltipCapture.begin(stack);
    }

    @Inject(
            method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("HEAD"),
            require = 0
    )
    private void exworld$captureStackAndLines(Font font, List<Component> text, Optional<?> data, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (stack != null && !stack.isEmpty()) TooltipCapture.begin(stack, text);
    }

    @Inject(
            method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V",
            at = @At("HEAD"),
            require = 0
    )
    private void exworld$captureLines(Font font, List<Component> text, Optional<?> data, int x, int y, CallbackInfo ci) {
        ItemStack stack = TooltipCapture.stack();
        if (stack.isEmpty() && tooltipStack != null && !tooltipStack.isEmpty()) {
            stack = tooltipStack;
        }
        if (!stack.isEmpty()) TooltipCapture.begin(stack, text);
    }

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"), cancellable = true)
    private void exworld$drawThemedTooltip(
            Font font,
            List<ClientTooltipComponent> components,
            int x,
            int y,
            ClientTooltipPositioner positioner,
            CallbackInfo ci
    ) {
        ItemStack stack = TooltipCapture.stack();
        if (stack.isEmpty() && tooltipStack != null && !tooltipStack.isEmpty()) {
            TooltipCapture.begin(tooltipStack);
            stack = tooltipStack;
        }
        if (stack.isEmpty()) return;
        if (TooltipRenderer.tryRender((GuiGraphics) (Object) this, font, components, x, y, positioner)) {
            ci.cancel();
        }
        TooltipCapture.end();
    }
}
