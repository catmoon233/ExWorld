package net.exmo.lotm;

import net.exmo.lotm.sequence.AttributeGrant;
import net.exmo.lotm.sequence.PathwayDefinition;
import net.exmo.lotm.sequence.RegisterSequencesEvent;
import net.exmo.lotm.sequence.SequenceRank;
import net.exmo.lotm.sequence.SequenceRegistry;
import net.exmo.lotm.sequence.SequenceSkill;
import net.exmo.lotm.spell.LotmSpells;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;

/** Demoness pathway, entered as the Assassin. Sequences 9-8 are the playable slice. */
public final class WitchPathway {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("lotm", "witch");

    private WitchPathway() {}

    @SubscribeEvent
    public static void register(RegisterSequencesEvent event) {
        register();
    }

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.witch")
                .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.witch.9", "sequence.lotm.witch.9.intro",
                        List.of(),
                        List.of(
                                SequenceSkill.active(LotmSpells.STEALTH.getId(), 1, "spell.lotm.stealth", "spell.lotm.stealth.guide"),
                                SequenceSkill.passive(LotmRuntime.SHADOW, "passive.lotm.shadow_affinity", "passive.lotm.shadow_affinity.desc", "minecraft:phantom_membrane")))
                .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.witch.8", "sequence.lotm.witch.8.intro",
                        List.of(AttributeGrant.add(key(Attributes.LUCK), 1)),
                        List.of(
                                SequenceSkill.active(LotmSpells.INSTIGATE.getId(), 1, "spell.lotm.instigate", "spell.lotm.instigate.guide"),
                                SequenceSkill.passive(LotmRuntime.CHARM, "passive.lotm.charm", "passive.lotm.charm.desc", "minecraft:emerald")))
                .sequence(SequenceRank.SEQUENCE_7, "sequence.lotm.witch.7", "sequence.lotm.witch.7.intro", List.of(), List.of())
                .sequence(SequenceRank.SEQUENCE_6, "sequence.lotm.witch.6", "sequence.lotm.witch.6.intro", List.of(), List.of())
                .sequence(SequenceRank.SEQUENCE_5, "sequence.lotm.witch.5", "sequence.lotm.witch.5.intro", List.of(), List.of())
                .sequence(SequenceRank.SEQUENCE_4, "sequence.lotm.witch.4", "sequence.lotm.witch.4.intro", List.of(), List.of())
                .sequence(SequenceRank.SEQUENCE_3, "sequence.lotm.witch.3", "sequence.lotm.witch.3.intro", List.of(), List.of())
                .sequence(SequenceRank.SEQUENCE_2, "sequence.lotm.witch.2", "sequence.lotm.witch.2.intro", List.of(), List.of())
                .sequence(SequenceRank.SEQUENCE_1, "sequence.lotm.witch.1", "sequence.lotm.witch.1.intro", List.of(), List.of())
                .sequence(SequenceRank.SEQUENCE_0, "sequence.lotm.witch.0", "sequence.lotm.witch.0.intro", List.of(), List.of())
                .sequence(SequenceRank.OLD_ONE, "sequence.lotm.witch.old_one", "sequence.lotm.witch.old_one.intro", List.of(), List.of())
                .sequence(SequenceRank.PILLAR, "sequence.lotm.witch.pillar", "sequence.lotm.witch.pillar.intro", List.of(), List.of())
                .build());
    }

    private static ResourceLocation key(Holder<Attribute> holder) {
        return holder.unwrapKey().orElseThrow().location();
    }
}
