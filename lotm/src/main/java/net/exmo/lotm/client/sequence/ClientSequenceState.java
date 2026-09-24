package net.exmo.lotm.client.sequence;

import net.exmo.lotm.network.SequencePayloads;
import net.exmo.lotm.sequence.ClientSequenceBridge;

import java.util.List;

public final class ClientSequenceState {
    private static SequencePayloads.SequenceSnapshotPayload snapshot = empty();

    private ClientSequenceState() {}

    public static SequencePayloads.SequenceSnapshotPayload snapshot() {
        return snapshot;
    }

    public static void install(SequencePayloads.SequenceSnapshotPayload next) {
        snapshot = next == null ? empty() : next;
    }

    public static void bind() {
        ClientSequenceBridge.bind(new ClientSequenceBridge.View() {
            @Override
            public boolean owns(String spellId) {
                if (spellId == null) return false;
                for (SequencePayloads.OwnedSpell spell : snapshot.ownedSpells()) {
                    if (spellId.equals(spell.spellId())) return true;
                }
                return false;
            }

            @Override
            public List<SequencePayloads.OwnedSpell> spells() {
                return snapshot.ownedSpells();
            }
        });
    }

    private static SequencePayloads.SequenceSnapshotPayload empty() {
        return new SequencePayloads.SequenceSnapshotPayload(false, false, "", "", "", "", List.of(), List.of());
    }
}
