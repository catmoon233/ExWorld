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

/** Hanged Man pathway. Sequences 9-8 are the implemented slice. */
public final class HangedManPathway {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("lotm", "hanged_man");

    private HangedManPathway() {}

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.hanged_man")
                .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.hanged_man.9", "sequence.lotm.hanged_man.9.intro",
                        List.of(),
                        List.of(
                                SequenceSkill.active(LotmSpells.STEAL_TOUCH.getId(), 1, "spell.lotm.steal_touch", "spell.lotm.steal_touch.guide"),
                                SequenceSkill.passive(HangedManPassives.HIDDEN, "passive.lotm.hidden_perception", "passive.lotm.hidden_perception.desc", "minecraft:ender_eye")))
                .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.hanged_man.8", "sequence.lotm.hanged_man.8.intro",
                        List.of(AttributeGrant.add(LotmSupport.attribute(Attributes.LUCK), 1.0)),
                        List.of(
                                SequenceSkill.active(LotmSpells.MAD_WHISPER.getId(), 1, "spell.lotm.mad_whisper", "spell.lotm.mad_whisper.guide"),
                                SequenceSkill.passive(HangedManPassives.SPIRIT, "passive.lotm.spiritual_intuition", "passive.lotm.spiritual_intuition.desc", "minecraft:rabbit_foot")))
                .build());
    }
}
