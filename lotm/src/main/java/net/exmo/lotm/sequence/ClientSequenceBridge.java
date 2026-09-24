package net.exmo.lotm.sequence;

import net.exmo.lotm.network.SequencePayloads;

import java.util.List;

/** Client view bound at startup. Stays empty on a dedicated server. */
public final class ClientSequenceBridge {
    public interface View {
        boolean owns(String spellId);

        List<SequencePayloads.OwnedSpell> spells();
    }

    private static View view = new View() {
        @Override
        public boolean owns(String spellId) {
            return false;
        }

        @Override
        public List<SequencePayloads.OwnedSpell> spells() {
            return List.of();
        }
    };

    private ClientSequenceBridge() {}

    public static void bind(View next) {
        if (next != null) view = next;
    }

    public static boolean owns(net.minecraft.resources.ResourceLocation spellId) {
        return spellId != null && view.owns(spellId.toString());
    }

    public static List<SequenceSkill> spells() {
        return view.spells().stream()
                .map(spell -> SequenceSkill.active(
                        net.minecraft.resources.ResourceLocation.parse(spell.spellId()),
                        spell.level(),
                        "",
                        ""))
                .toList();
    }
}
