package net.exmo.lotm;

import net.exmo.lotm.sequence.AttributeGrant;
import net.exmo.lotm.sequence.PathwayDefinition;
import net.exmo.lotm.sequence.SequenceRank;
import net.exmo.lotm.sequence.SequenceRegistry;
import net.exmo.lotm.sequence.SequenceSkill;
import net.exmo.lotm.spell.LotmSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/** Spectator pathway, epithet Visionary. Sequences 9-8 are the implemented slice. */
public final class SpectatorPathway {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("lotm", "spectator");

    private SpectatorPathway() {}

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.spectator")
                .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.spectator.9", "sequence.lotm.spectator.9.intro",
                        List.of(AttributeGrant.add(LotmSupport.attribute(Attributes.LUCK), 2.0)),
                        List.of(
                                SequenceSkill.active(LotmSpells.SUBTLE_GUIDANCE.getId(), 1, "spell.lotm.subtle_guidance", "spell.lotm.subtle_guidance.guide"),
                                SequenceSkill.passive(SpectatorPassives.KEEN_OBSERVATION, "passive.lotm.keen_observation", "passive.lotm.keen_observation.desc", "minecraft:spyglass")))
                .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.spectator.8", "sequence.lotm.spectator.8.intro",
                        List.of(AttributeGrant.add(LotmSupport.spellPower(), 0.05)),
                        List.of(
                                SequenceSkill.active(LotmSpells.MIND_READ.getId(), 1, "spell.lotm.mind_read", "spell.lotm.mind_read.guide"),
                                SequenceSkill.active(LotmSpells.MIND_MIMIC.getId(), 2, "spell.lotm.mind_mimic", "spell.lotm.mind_mimic.guide"),
                                SequenceSkill.passive(SpectatorPassives.MIND_INTUITION, "passive.lotm.mind_intuition", "passive.lotm.mind_intuition.desc", "minecraft:ender_eye")))
                .build());
    }
}
