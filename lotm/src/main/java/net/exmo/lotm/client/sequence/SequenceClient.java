package net.exmo.lotm.client.sequence;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.exmo.exworld.client.inventory.BackpackTabs;
import net.exmo.lotm.network.SequencePayloads;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SequenceClient {
    private SequenceClient() {}

    public static void register() {
        ClientSequenceState.bind();
        BackpackTabs.setSequenceOpen(() -> PacketDistributor.sendToServer(new SequencePayloads.OpenSequencePayload()));
    }

    public static void receive(SequencePayloads.SequenceSnapshotPayload payload) {
        ClientSequenceState.install(payload);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            ClientMagicData.updateSpellSelectionManager();
        }
        if (payload.open()) {
            if (!(minecraft.screen instanceof SequenceScreen)) {
                minecraft.setScreen(new SequenceScreen());
            }
        }
    }
}
