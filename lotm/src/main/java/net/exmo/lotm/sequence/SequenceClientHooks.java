package net.exmo.lotm.sequence;

import net.exmo.lotm.network.SequencePayloads;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;

public final class SequenceClientHooks {
    private SequenceClientHooks() {}

    public static void receive(SequencePayloads.SequenceSnapshotPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.exmo.lotm.client.sequence.SequenceClient.receive(payload);
        }
    }
}
