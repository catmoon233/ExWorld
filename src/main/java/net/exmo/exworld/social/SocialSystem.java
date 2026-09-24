package net.exmo.exworld.social;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

/** Chat audience rules and the ExWorld command guide. */
public final class SocialSystem {
    private SocialSystem() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(SocialSystem.class);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        ExworldHelp.register(event);
    }

    @SubscribeEvent
    public static void chat(ServerChatEvent event) {
        NearbyChat.deliver(event);
    }
}
