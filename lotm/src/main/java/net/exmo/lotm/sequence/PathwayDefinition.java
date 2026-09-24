package net.exmo.lotm.sequence;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record PathwayDefinition(ResourceLocation id, String nameKey, List<SequenceDefinition> sequences) {
    public PathwayDefinition {
        sequences = sequences == null ? List.of() : sequences.stream()
                .sorted(Comparator.comparingInt(sequence -> sequence.rank().power()))
                .toList();
    }

    public SequenceDefinition byRank(SequenceRank rank) {
        if (rank == null) return null;
        for (SequenceDefinition sequence : sequences) {
            if (sequence.rank() == rank) return sequence;
        }
        return null;
    }

    /** Sequences from 9 up to and including the current rank. */
    public List<SequenceDefinition> unlockedThrough(SequenceRank current) {
        if (current == null) return List.of();
        List<SequenceDefinition> unlocked = new ArrayList<>();
        for (SequenceDefinition sequence : sequences) {
            if (sequence.rank().unlockedWhenCurrentIs(current)) unlocked.add(sequence);
        }
        return unlocked;
    }

    public static Builder builder(ResourceLocation id, String nameKey) {
        return new Builder(id, nameKey);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final String nameKey;
        private final List<SequenceDefinition> sequences = new ArrayList<>();

        private Builder(ResourceLocation id, String nameKey) {
            this.id = id;
            this.nameKey = nameKey;
        }

        public Builder sequence(SequenceRank rank, String nameKey, String introductionKey,
                                List<AttributeGrant> attributes, List<SequenceSkill> skills) {
            ResourceLocation sequenceId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "/" + rank.token());
            sequences.add(new SequenceDefinition(sequenceId, id, rank, nameKey, introductionKey, attributes, skills));
            return this;
        }

        public PathwayDefinition build() {
            return new PathwayDefinition(id, nameKey, sequences);
        }
    }
}
