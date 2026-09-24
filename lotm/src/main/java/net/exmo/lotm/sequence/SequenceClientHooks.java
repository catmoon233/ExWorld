package net.exmo.lotm.sequence;

import net.exmo.lotm.network.SequencePayloads;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

public final class SequenceClientHooks {
    private SequenceClientHooks() {}

    public static void registerClient(IEventBus modBus) {
        if (FMLEnvironment.dist != Dist.CLIENT || modBus == null) return;
        modBus.addListener(SequenceClientHooks::client);
    }

    private static void client(FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.exmo.lotm.client.sequence.SequenceClient.register());
    }

    public static void receive(SequencePayloads.SequenceSnapshotPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.exmo.lotm.client.sequence.SequenceClient.receive(payload);
        }
    }
}
