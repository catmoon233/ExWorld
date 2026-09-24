package net.exmo.lotm.sequence;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class SequenceRegistry {
    private static final Map<ResourceLocation, PathwayDefinition> PATHWAYS = new LinkedHashMap<>();

    private SequenceRegistry() {}

    public static void register(PathwayDefinition pathway) {
        if (pathway == null || pathway.id() == null) return;
        PATHWAYS.put(pathway.id(), pathway);
    }

    public static Optional<PathwayDefinition> pathway(ResourceLocation id) {
        return Optional.ofNullable(PATHWAYS.get(id));
    }

    public static Optional<PathwayDefinition> findPathway(ResourceLocation id) {
        if (id == null) return Optional.empty();
        PathwayDefinition exact = PATHWAYS.get(id);
        if (exact != null) return Optional.of(exact);
        return findPathway(id.getPath());
    }

    public static Optional<PathwayDefinition> findPathway(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String token = raw.trim();
        if (token.indexOf(':') >= 0) {
            try {
                PathwayDefinition exact = PATHWAYS.get(ResourceLocation.parse(token));
                if (exact != null) return Optional.of(exact);
            } catch (RuntimeException ignored) {
                return Optional.empty();
            }
        }
        for (PathwayDefinition pathway : PATHWAYS.values()) {
            if (pathway.id().getPath().equals(token) || pathway.id().toString().equals(token)) {
                return Optional.of(pathway);
            }
        }
        return Optional.empty();
    }

    public static Collection<PathwayDefinition> pathways() {
        return PATHWAYS.values();
    }

    public static Collection<ResourceLocation> pathwayIds() {
        return PATHWAYS.keySet();
    }

    public static Collection<String> pathwaySuggestions() {
        return PATHWAYS.keySet().stream().map(ResourceLocation::toString).toList();
    }

    public static Collection<String> rankSuggestions(ResourceLocation pathwayId) {
        return findPathway(pathwayId)
                .map(pathway -> pathway.sequences().stream().map(sequence -> sequence.rank().token()).toList())
                .orElseGet(() -> java.util.List.of(SequenceRank.tokens()));
    }

    public static Collection<String> rankSuggestions(String pathwayRaw) {
        return findPathway(pathwayRaw)
                .map(pathway -> pathway.sequences().stream().map(sequence -> sequence.rank().token()).toList())
                .orElseGet(() -> java.util.List.of(SequenceRank.tokens()));
    }
}
