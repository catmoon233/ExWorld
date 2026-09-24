package net.exmo.lotm.client;

import net.exmo.exworld.client.inventory.BackpackTabs;
import net.exmo.lotm.client.sequence.SequenceClient;
import net.exmo.lotm.network.SequencePayloads;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LotmClient {
    private LotmClient() {}

    public static void bootstrap() {
        SequenceClient.register();
        BackpackTabs.setSequenceOpen(() -> PacketDistributor.sendToServer(new SequencePayloads.OpenSequencePayload()));
        NeoForge.EVENT_BUS.addListener(ColorSenseClient::render);
        NeoForge.EVENT_BUS.addListener(ColorSenseClient::hud);
        NeoForge.EVENT_BUS.addListener(TrueSightClient::before);
        NeoForge.EVENT_BUS.addListener(TrueSightClient::after);
        NeoForge.EVENT_BUS.addListener(KeenObservationClient::tick);
        NeoForge.EVENT_BUS.addListener(ResourceSenseClient::tick);
        NeoForge.EVENT_BUS.addListener(SpiritSenseClient::render);
        NeoForge.EVENT_BUS.addListener(SpiritSenseClient::hud);
        NeoForge.EVENT_BUS.addListener(MindScrambleClient::input);
        NeoForge.EVENT_BUS.addListener(SpiritVisionClient::tick);
        NeoForge.EVENT_BUS.addListener(SpiritVisionClient::render);
        NeoForge.EVENT_BUS.addListener(net.exmo.lotm.client.phone.MapWandOverlay::render);
    }
}
