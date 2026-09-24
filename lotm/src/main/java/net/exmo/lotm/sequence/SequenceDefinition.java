package net.exmo.lotm.sequence;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SequenceDefinition(
        ResourceLocation id,
        ResourceLocation pathwayId,
        SequenceRank rank,
        String nameKey,
        String introductionKey,
        List<AttributeGrant> attributes,
        List<SequenceSkill> skills
) {
    public SequenceDefinition {
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
        skills = skills == null ? List.of() : List.copyOf(skills);
    }
}
