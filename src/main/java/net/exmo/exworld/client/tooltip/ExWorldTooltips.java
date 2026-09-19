package net.exmo.exworld.client.tooltip;

import net.exmo.exmodifier.client.ExModifierClient;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client bootstrap for the ExWorld item tooltip. */
public final class ExWorldTooltips {
    private ExWorldTooltips() {}

    public static void register() {
        ExModifierClient.setVanillaItemTooltipDump(false);
        NeoForge.EVENT_BUS.addListener(ExWorldTooltips::tick);
        NeoForge.EVENT_BUS.addListener(ExWorldTooltips::scroll);
    }

    private static void tick(ClientTickEvent.Post event) {
        TooltipCapture.tick();
    }

    private static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        if (TooltipCapture.scrollBy(event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }
}
