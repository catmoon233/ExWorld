package net.exmo.lotm.story;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class StorySystem {
    private StorySystem() {}

    public static void register(IEventBus bus) {
        StoryItems.register(bus);
        bus.addListener(StorySystem::payloads);
        NeoForge.EVENT_BUS.register(StoryCommands.class);
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("3");
        registrar.playToServer(StoryPayloads.SendLetterPayload.TYPE, StoryPayloads.SendLetterPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) StoryActions.sendLetter(player, payload.text());
                }));
        registrar.playToServer(StoryPayloads.BuyPayload.TYPE, StoryPayloads.BuyPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) StoryActions.buy(player, payload.offer());
                }));
        registrar.playToClient(StoryPayloads.OpenLetterPayload.TYPE, StoryPayloads.OpenLetterPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> StoryClientHooks.openLetter.accept(payload.text())));
    }
}
