package net.exmo.lotm;

import net.exmo.lotm.sequence.AttributeGrant;
import net.exmo.lotm.sequence.PathwayDefinition;
import net.exmo.lotm.sequence.SequenceRank;
import net.exmo.lotm.sequence.SequenceRegistry;
import net.exmo.lotm.sequence.SequenceSkill;
import net.exmo.lotm.spell.LotmSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/** Error pathway. Sequences 9-8 are the implemented slice. */
public final class ThiefPathway {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("lotm", "thief");

    private ThiefPathway() {}

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.thief")
                .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.thief.9", "sequence.lotm.thief.9.intro",
                        List.of(
                                new AttributeGrant(LotmSupport.attribute(Attributes.ATTACK_SPEED), 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                                // Design note +0.5 would be six times walking speed. 0.05 matches the other pathways.
                                AttributeGrant.add(LotmSupport.attribute(Attributes.MOVEMENT_SPEED), 0.05)),
                        List.of(
                                SequenceSkill.active(LotmSpells.THIEF_TOUCH.getId(), 1, "spell.lotm.thief_touch", "spell.lotm.thief_touch.guide"),
                                SequenceSkill.passive(ThiefPassives.AGILE, "passive.lotm.agile_hands", "passive.lotm.agile_hands.desc", "minecraft:iron_sword")))
                .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.thief.8", "sequence.lotm.thief.8.intro",
                        List.of(AttributeGrant.add(LotmSupport.attribute(Attributes.LUCK), 1.0)),
                        List.of(
                                SequenceSkill.active(LotmSpells.MIND_INTERFERENCE.getId(), 1, "spell.lotm.mind_interference", "spell.lotm.mind_interference.guide"),
                                SequenceSkill.active(LotmSpells.THOUGHT_MISLEAD.getId(), 1, "spell.lotm.thought_mislead", "spell.lotm.thought_mislead.guide"),
                                SequenceSkill.passive(ThiefPassives.ELOQUENCE, "passive.lotm.eloquence", "passive.lotm.eloquence.desc", "minecraft:emerald")))
                .build());
    }
}
