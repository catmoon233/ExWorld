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

/** Door pathway. Sequences 9-8 are the implemented slice. */
public final class ApprenticePathway {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("lotm", "apprentice");

    private ApprenticePathway() {}

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.apprentice")
                .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.apprentice.9", "sequence.lotm.apprentice.9.intro",
                        List.of(new AttributeGrant(LotmSupport.attribute(Attributes.MOVEMENT_SPEED), 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)),
                        List.of(
                                SequenceSkill.active(LotmSpells.OPEN_DOOR.getId(), 1, "spell.lotm.open_door", "spell.lotm.open_door.guide"),
                                SequenceSkill.passive(ApprenticePassives.SPIRIT, "passive.lotm.spirit_sense", "passive.lotm.spirit_sense.desc", "minecraft:ender_eye")))
                .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.apprentice.8", "sequence.lotm.apprentice.8.intro",
                        List.of(AttributeGrant.add(LotmSupport.attribute(Attributes.MOVEMENT_SPEED), 0.05)),
                        List.of(
                                SequenceSkill.active(LotmSpells.FLASH.getId(), 1, "spell.lotm.flash", "spell.lotm.flash.guide"),
                                SequenceSkill.active(LotmSpells.LOUD_NOISE.getId(), 1, "spell.lotm.loud_noise", "spell.lotm.loud_noise.guide"),
                                SequenceSkill.passive(ApprenticePassives.SPATIAL, "passive.lotm.spatial_intuition", "passive.lotm.spatial_intuition.desc", "minecraft:ender_pearl")))
                .build());
    }
}
