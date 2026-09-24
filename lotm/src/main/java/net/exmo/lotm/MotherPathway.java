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

/** Great Mother pathway. Sequences 9-8 are the implemented slice. */
public final class MotherPathway {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("lotm", "mother");

    private MotherPathway() {}

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.mother")
                .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.mother.9", "sequence.lotm.mother.9.intro",
                        List.of(AttributeGrant.add(key(Attributes.MAX_HEALTH), 2.0)),
                        List.of(
                                SequenceSkill.active(LotmSpells.NATURAL_GROWTH.getId(), 1, "spell.lotm.natural_growth", "spell.lotm.natural_growth.guide"),
                                SequenceSkill.passive(MotherPassives.EARTH, "passive.lotm.earth_power", "passive.lotm.earth_power.desc", "minecraft:grass_block")))
                .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.mother.8", "sequence.lotm.mother.8.intro",
                        List.of(),
                        List.of(
                                SequenceSkill.active(LotmSpells.HEALING_HANDS.getId(), 1, "spell.lotm.healing_hands", "spell.lotm.healing_hands.guide"),
                                SequenceSkill.passive(MotherPassives.AFFINITY, "passive.lotm.potion_affinity", "passive.lotm.potion_affinity.desc", "minecraft:potion")))
                .build());
    }

    private static ResourceLocation key(Holder<Attribute> holder) {
        return holder.unwrapKey().orElseThrow().location();
    }
}
