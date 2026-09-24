package net.exmo.exworld.npc.logic;

import net.exmo.exworld.npc.data.RelationEdge;

import java.util.List;
import java.util.Optional;

/** Directed relation lookup. A missing NPC or edge is empty, never an error. */
public final class RelationGraph {
    private final List<RelationEdge> edges;

    public RelationGraph(List<RelationEdge> edges) {
        this.edges = edges == null ? List.of() : List.copyOf(edges);
    }

    public Optional<RelationEdge> find(String fromId, String toId) {
        if (fromId == null || toId == null || fromId.isBlank() || toId.isBlank()) return Optional.empty();
        for (RelationEdge edge : edges) {
            if (fromId.equals(edge.fromId()) && toId.equals(edge.toId())) return Optional.of(edge);
        }
        return Optional.empty();
    }

    public List<RelationEdge> outgoing(String fromId) {
        if (fromId == null || fromId.isBlank()) return List.of();
        return edges.stream().filter(edge -> fromId.equals(edge.fromId())).toList();
    }

    public String summary(String fromId) {
        StringBuilder text = new StringBuilder();
        for (RelationEdge edge : outgoing(fromId)) {
            if (!text.isEmpty()) text.append("; ");
            text.append(edge.toId()).append(' ').append(edge.type()).append(' ').append(edge.affinity());
        }
        return text.toString();
    }
}
