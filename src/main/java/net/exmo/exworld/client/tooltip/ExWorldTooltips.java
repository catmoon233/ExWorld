package net.exmo.exworld.client.tooltip;

import net.exmo.exmodifier.client.ExModifierClient;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client bootstrap for the ExWorld item tooltip.
 * Rendering is hooked through {@link RenderTooltipEvent.Pre} so {@code GuiGraphics}
 * is never mixed in during FML's early display window (which cannot see mod classes).
 */
public final class ExWorldTooltips {
    private ExWorldTooltips() {}

    public static void register() {
        ExModifierClient.setVanillaItemTooltipDump(false);
        NeoForge.EVENT_BUS.addListener(ExWorldTooltips::tick);
        NeoForge.EVENT_BUS.addListener(ExWorldTooltips::scroll);
        NeoForge.EVENT_BUS.addListener(ExWorldTooltips::render);
    }

    private static void tick(ClientTickEvent.Post event) {
        TooltipCapture.tick();
    }

    private static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        if (TooltipCapture.scrollBy(event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    private static void render(RenderTooltipEvent.Pre event) {
        ItemStack stack = event.getItemStack();
        if (stack == null || stack.isEmpty()) return;
        TooltipCapture.begin(stack);
        var positioner = event.getTooltipPositioner();
        if (positioner == null) positioner = DefaultTooltipPositioner.INSTANCE;
        if (TooltipRenderer.tryRender(
                event.getGraphics(),
                event.getFont(),
                event.getComponents(),
                event.getX(),
                event.getY(),
                positioner
        )) {
            event.setCanceled(true);
        }
        TooltipCapture.end();
    }
}
