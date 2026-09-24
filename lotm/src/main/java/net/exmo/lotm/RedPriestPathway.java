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

 /** Red Priest pathway. Sequences 9-8 follow the hunter / provoker slice. */
public final class RedPriestPathway {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("lotm", "red_priest");

    private RedPriestPathway() {}

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.red_priest")
                 .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.red_priest.9", "sequence.lotm.red_priest.9.intro",
                         List.of(AttributeGrant.add(LotmSupport.attribute(Attributes.ATTACK_DAMAGE), 2.0)),
                         List.of(
                                 SequenceSkill.active(LotmSpells.PLACE_TRAP.getId(), 1, "spell.lotm.place_trap", "spell.lotm.place_trap.guide"),
                                 SequenceSkill.passive(HunterPassives.EYE, "passive.lotm.hunters_eye", "passive.lotm.hunters_eye.desc", "minecraft:spyglass")))
                 .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.red_priest.8", "sequence.lotm.red_priest.8.intro",
                         List.of(),
                         List.of(
                                 SequenceSkill.active(LotmSpells.TAUNT.getId(), 1, "spell.lotm.taunt", "spell.lotm.taunt.guide"),
                                 SequenceSkill.passive(HunterPassives.REFLEX, "passive.lotm.provoke_reflex", "passive.lotm.provoke_reflex.desc", "minecraft:shield")))
                .build());
    }
}
