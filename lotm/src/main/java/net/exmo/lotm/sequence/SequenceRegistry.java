package net.exmo.lotm.sequence;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SequenceRegistry {
    private static final Map<ResourceLocation, PathwayDefinition> PATHWAYS = new LinkedHashMap<>();
    private static final Map<String, ResourceLocation> ALIASES = new LinkedHashMap<>();

    private SequenceRegistry() {}

    public static void register(PathwayDefinition pathway) {
        if (pathway == null || pathway.id() == null) return;
        PATHWAYS.put(pathway.id(), pathway);
    }

    public static void alias(String token, ResourceLocation canonical) {
        if (token == null || token.isBlank() || canonical == null) return;
        ALIASES.put(token.toLowerCase(Locale.ROOT), canonical);
    }

    public static Optional<PathwayDefinition> pathway(ResourceLocation id) {
        return Optional.ofNullable(PATHWAYS.get(id));
    }

    public static Optional<PathwayDefinition> findPathway(ResourceLocation id) {
        if (id == null) return Optional.empty();
        PathwayDefinition exact = PATHWAYS.get(id);
        if (exact != null) return Optional.of(exact);
        Optional<PathwayDefinition> aliased = aliased(id.toString());
        if (aliased.isPresent()) return aliased;
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
            Optional<PathwayDefinition> namespaced = aliased(token);
            if (namespaced.isPresent()) return namespaced;
        }
        Optional<PathwayDefinition> aliased = aliased(token);
        if (aliased.isPresent()) return aliased;
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
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        PATHWAYS.keySet().forEach(id -> ids.add(id.toString()));
        for (String token : ALIASES.keySet()) {
            if (token.indexOf(':') >= 0) ids.add(token);
        }
        return ids;
    }

    private static Optional<PathwayDefinition> aliased(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        ResourceLocation id = ALIASES.get(token.toLowerCase(Locale.ROOT));
        return id == null ? Optional.empty() : Optional.ofNullable(PATHWAYS.get(id));
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
