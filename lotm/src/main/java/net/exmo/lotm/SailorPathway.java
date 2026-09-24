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

/** Sea God pathway, sequence 9 Sailor. Sequences 9-8 are the implemented slice. */
public final class SailorPathway {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("lotm", "sailor");

    private SailorPathway() {}

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.sailor")
                .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.sailor.9", "sequence.lotm.sailor.9.intro",
                        List.of(
                                AttributeGrant.add(LotmSupport.attribute(Attributes.MAX_HEALTH), 10),
                                AttributeGrant.add(LotmSupport.attribute(Attributes.SUBMERGED_MINING_SPEED), 0.8)),
                        List.of(
                                SequenceSkill.active(LotmSpells.SEA_BLESSING.getId(), 1, "spell.lotm.sea_blessing", "spell.lotm.sea_blessing.guide"),
                                SequenceSkill.passive(SailorPassives.OCEAN, "passive.lotm.ocean_affinity", "passive.lotm.ocean_affinity.desc", "minecraft:heart_of_the_sea")))
                .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.sailor.8", "sequence.lotm.sailor.8.intro",
                        List.of(AttributeGrant.add(LotmSupport.attribute(Attributes.ARMOR), 1)),
                        List.of(
                                SequenceSkill.active(LotmSpells.STORM_WRATH.getId(), 1, "spell.lotm.storm_wrath", "spell.lotm.storm_wrath.guide"),
                                SequenceSkill.passive(SailorPassives.STORM_GUARD, "passive.lotm.storm_guard", "passive.lotm.storm_guard.desc", "minecraft:shield")))
                .build());
    }
}
