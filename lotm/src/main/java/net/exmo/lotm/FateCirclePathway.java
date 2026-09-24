package net.exmo.lotm;

import net.exmo.lotm.sequence.AttributeGrant;
import net.exmo.lotm.sequence.PathwayDefinition;
import net.exmo.lotm.sequence.SequenceRank;
import net.exmo.lotm.sequence.SequenceRegistry;
import net.exmo.lotm.sequence.SequenceSkill;
import net.exmo.lotm.spell.LotmSpells;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/** Wheel of Fate pathway. Sequences 9-8 are the implemented slice. */
public final class FateCirclePathway {
    public static final net.minecraft.resources.ResourceLocation ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("lotm", "fate_circle");

    private FateCirclePathway() {}

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.fate_circle")
                .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.fate_circle.9", "sequence.lotm.fate_circle.9.intro",
                        List.of(
                                AttributeGrant.add(LotmSupport.attribute(Attributes.ENTITY_INTERACTION_RANGE), 2.0),
                                AttributeGrant.add(LotmSupport.attribute(Attributes.LUCK), 1.0)),
                        List.of(
                                SequenceSkill.active(LotmSpells.RESOURCE_SENSE.getId(), 1, "spell.lotm.resource_sense", "spell.lotm.resource_sense.guide"),
                                SequenceSkill.passive(FateCirclePassives.KEEN_SENSES, "passive.lotm.keen_senses", "passive.lotm.keen_senses.desc", "minecraft:spyglass")))
                .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.fate_circle.8", "sequence.lotm.fate_circle.8.intro",
                        List.of(),
                        List.of(
                                SequenceSkill.active(LotmSpells.PRECISE_HARVEST.getId(), 1, "spell.lotm.precise_harvest", "spell.lotm.precise_harvest.guide"),
                                SequenceSkill.passive(FateCirclePassives.DANGER_SENSE, "passive.lotm.danger_premonition", "passive.lotm.danger_premonition.desc", "minecraft:clock")))
                .build());
    }
}
