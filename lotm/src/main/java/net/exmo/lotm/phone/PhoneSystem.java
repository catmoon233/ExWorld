package net.exmo.lotm.phone;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PhoneSystem {
    private PhoneSystem() {}

    public static void register(IEventBus bus) {
        LotmBlocks.register(bus);
        LotmItems.register(bus);
        bus.addListener(PhoneSystem::payloads);
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(PhonePayloads.PhoneActionPayload.TYPE, PhonePayloads.PhoneActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) PhoneActions.handle(player, payload.json());
                }));
        if (FMLEnvironment.dist == Dist.CLIENT) PhoneClient.register(registrar);
    }

    static void send(ServerPlayer player, String json) {
        PacketDistributor.sendToPlayer(player, new PhonePayloads.PhoneStatePayload(json));
    }
}
