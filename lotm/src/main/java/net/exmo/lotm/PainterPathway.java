package net.exmo.lotm;

import net.exmo.lotm.sequence.AttributeGrant;
import net.exmo.lotm.sequence.PathwayDefinition;
import net.exmo.lotm.sequence.SequenceRank;
import net.exmo.lotm.sequence.SequenceRegistry;
import net.exmo.lotm.sequence.SequenceSkill;
import net.exmo.lotm.spell.LotmSpells;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/** Painter pathway, epithet High-Dimensional Overlooker. Sequences 9-8 are the implemented slice. */
public final class PainterPathway {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("lotm", "painter");

    private PainterPathway() {}

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.painter")
                .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.painter.9", "sequence.lotm.painter.9.intro",
                        List.of(AttributeGrant.add(key(Attributes.LUCK), 1.0)),
                        List.of(
                                SequenceSkill.active(LotmSpells.SPIRITUAL_GRAFFITI.getId(), 1, "spell.lotm.spiritual_graffiti", "spell.lotm.spiritual_graffiti.guide"),
                                SequenceSkill.passive(PainterPassives.COLOR_SENSE, "passive.lotm.color_sense", "passive.lotm.color_sense.desc", "minecraft:spyglass")))
                .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.painter.8", "sequence.lotm.painter.8.intro",
                        List.of(),
                        List.of(
                                SequenceSkill.active(LotmSpells.TRUE_SIGHT.getId(), 1, "spell.lotm.true_sight", "spell.lotm.true_sight.guide"),
                                SequenceSkill.active(LotmSpells.SWIFT.getId(), 1, "spell.lotm.swift", "spell.lotm.swift.guide"),
                                SequenceSkill.passive(PainterPassives.RECORDER, "passive.lotm.recorder", "passive.lotm.recorder.desc", "minecraft:writable_book")))
                .build());
    }

    private static ResourceLocation key(Holder<Attribute> holder) {
        return holder.unwrapKey().orElseThrow().location();
    }
}
