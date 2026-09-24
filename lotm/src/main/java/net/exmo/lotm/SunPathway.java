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

/** Sun pathway. Sequences 9-8 are the implemented slice. */
public final class SunPathway {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("lotm", "sun");

    private SunPathway() {}

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.sun")
                .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.sun.9", "sequence.lotm.sun.9.intro",
                        List.of(AttributeGrant.add(LotmSupport.attribute(Attributes.MAX_HEALTH), 10.0)),
                        List.of(
                                SequenceSkill.active(LotmSpells.HYMN.getId(), 1, "spell.lotm.hymn", "spell.lotm.hymn.guide"),
                                SequenceSkill.passive(SunPassives.COURAGE, "passive.lotm.courage_resonance", "passive.lotm.courage_resonance.desc", "minecraft:golden_apple")))
                .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.sun.8", "sequence.lotm.sun.8.intro",
                        List.of(AttributeGrant.add(LotmSupport.attribute(Attributes.ARMOR), 2.0)),
                        List.of(
                                SequenceSkill.active(LotmSpells.PURIFYING_LIGHT.getId(), 1, "spell.lotm.purifying_light", "spell.lotm.purifying_light.guide"),
                                SequenceSkill.active(LotmSpells.EXORCISM.getId(), 2, "spell.lotm.exorcism", "spell.lotm.exorcism.guide"),
                                SequenceSkill.passive(SunPassives.HOLY, "passive.lotm.holy_affinity", "passive.lotm.holy_affinity.desc", "minecraft:glowstone")))
                .build());
    }
}
