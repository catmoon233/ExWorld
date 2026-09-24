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

/** Wizard pathway, sequence 9 Mystery Pryer. Sequences 9-8 are the implemented slice. */
public final class WizardPathway {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("lotm", "wizard");

    private WizardPathway() {}

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.wizard")
                .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.wizard.9", "sequence.lotm.wizard.9.intro",
                        List.of(AttributeGrant.add(LotmSupport.spellPower(), 0.05)),
                        List.of(
                                SequenceSkill.active(LotmSpells.SPIRIT_VISION.getId(), 1, "spell.lotm.spirit_vision", "spell.lotm.spirit_vision.guide"),
                                SequenceSkill.passive(WizardPassives.LORE, "passive.lotm.mystic_lore", "passive.lotm.mystic_lore.desc", "minecraft:book")))
                .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.wizard.8", "sequence.lotm.wizard.8.intro",
                        List.of(AttributeGrant.add(LotmSupport.attribute(Attributes.ATTACK_DAMAGE), 3)),
                        List.of(
                                SequenceSkill.active(LotmSpells.KNOWLEDGE_STRIKE.getId(), 1, "spell.lotm.knowledge_strike", "spell.lotm.knowledge_strike.guide"),
                                SequenceSkill.passive(WizardPassives.ANALYSIS, "passive.lotm.combat_analysis", "passive.lotm.combat_analysis.desc", "minecraft:iron_sword")))
                .build());
    }
}
