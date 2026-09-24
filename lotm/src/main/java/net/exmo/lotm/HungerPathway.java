package net.exmo.lotm;

import net.exmo.lotm.sequence.AttributeGrant;
import net.exmo.lotm.sequence.PathwayDefinition;
import net.exmo.lotm.sequence.SequenceRank;
import net.exmo.lotm.sequence.SequenceRegistry;
import net.exmo.lotm.sequence.SequenceSkill;
import net.exmo.lotm.spell.LotmSpells;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/** Primordial Hunger pathway. Sequences 9-8 are the implemented slice. */
public final class HungerPathway {
    public static final net.minecraft.resources.ResourceLocation ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("lotm", "primordial_hunger");

    private HungerPathway() {}

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.primordial_hunger")
                .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.primordial_hunger.9", "sequence.lotm.primordial_hunger.9.intro",
                        List.of(AttributeGrant.add(LotmSupport.attribute(Attributes.ARMOR), 2.0)),
                        List.of(
                                SequenceSkill.active(LotmSpells.STREET_BRAWL.getId(), 1, "spell.lotm.street_brawl", "spell.lotm.street_brawl.guide"),
                                SequenceSkill.passive(HungerPassives.ADAPT, "passive.lotm.environment_adapt", "passive.lotm.environment_adapt.desc", "minecraft:leather_boots")))
                .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.primordial_hunger.8", "sequence.lotm.primordial_hunger.8.intro",
                        List.of(AttributeGrant.add(LotmSupport.attribute(Attributes.MAX_HEALTH), 2.0)),
                        List.of(
                                SequenceSkill.active(LotmSpells.DEVOUR.getId(), 1, "spell.lotm.devour", "spell.lotm.devour.guide"),
                                SequenceSkill.passive(HungerPassives.TOXIN, "passive.lotm.toxin_resist", "passive.lotm.toxin_resist.desc", "minecraft:milk_bucket")))
                .build());
    }
}
