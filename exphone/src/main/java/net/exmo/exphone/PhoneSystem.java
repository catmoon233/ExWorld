package net.exmo.exphone;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PhoneSystem {
    private PhoneSystem() {}

    public static void register(IEventBus bus) {
        PhoneBlocks.register(bus);
        PhoneItems.register(bus);
        PhoneSounds.register(bus);
        net.exmo.exphone.command.TokenArgument.register(bus);
        NeoForge.EVENT_BUS.register(PhoneCommands.class);
        NeoForge.EVENT_BUS.addListener(PhoneCalls::tick);
        NeoForge.EVENT_BUS.addListener(PhoneCalls::logout);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            bus.addListener(net.exmo.exphone.client.PhoneItemColors::items);
            bus.addListener(PhoneCallHud::registerLayer);
            NeoForge.EVENT_BUS.addListener(net.exmo.exphone.client.MapWandOverlay::render);
        }
        bus.addListener(PhoneSystem::payloads);
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(PhonePayloads.PhoneActionPayload.TYPE, PhonePayloads.PhoneActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) PhoneActions.handle(player, payload.json());
                }));
        registrar.playToClient(PhonePayloads.PhoneStatePayload.TYPE, PhonePayloads.PhoneStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> PhoneClientHooks.receive(payload)));
        registrar.playBidirectional(PhonePhotoPayload.TYPE, PhonePhotoPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) PhonePhotos.receive(player, payload);
            else PhonePhotoClient.receive(payload);
        }));
        registrar.playBidirectional(PhoneAudioPayload.TYPE, PhoneAudioPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) PhoneAudioServer.receive(player, payload);
            else PhoneRecorder.receive(payload);
        }));
    }

    static void send(ServerPlayer player, String json) {
        PhoneProtocol.send(player, json);
    }
}
