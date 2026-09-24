package net.exmo.lotm.client.sequence;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.exmo.lotm.network.SequencePayloads;
import net.minecraft.client.Minecraft;

public final class SequenceClient {
    private SequenceClient() {}

    public static void register() {
        ClientSequenceState.bind();
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
