package net.exmo.exworld.npc.data;

import net.exmo.exworld.npc.marginal.MarginalRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Server-side checks before a document replaces the saved copy. */
public final class NpcValidator {
    private NpcValidator() {}

    public static String validate(NpcDocument doc, List<RelationEdge> edges) {
        if (doc == null) return "missing document";
        if (!doc.id().matches("[A-Za-z0-9_.:-]{1,64}")) return "invalid id";
        Set<String> placeIds = new HashSet<>();
        for (NpcPlace place : doc.places()) {
            if (place.id().isBlank() || !placeIds.add(place.id())) return "duplicate or blank place";
        }
        if (!doc.homePlaceId().isBlank() && !placeIds.contains(doc.homePlaceId())) return "home place is missing";
        Set<String> routeIds = new HashSet<>();
        for (NpcRoute route : doc.routes()) {
            if (route.id().isBlank() || !routeIds.add(route.id())) return "duplicate or blank route";
        }
        if (!doc.activeRouteId().isBlank() && !routeIds.contains(doc.activeRouteId())) return "active route is missing";
        Set<String> actionIds = new HashSet<>();
        for (ActionSpec action : doc.actions()) {
            if (action.id().isBlank() || !actionIds.add(action.id())) return "duplicate or blank action";
        }
        for (TimelineNode node : doc.timeline()) {
            if (node.durationMinutes() <= 0) return "timeline duration must be positive";
            if (!node.placeId().isBlank() && !placeIds.contains(node.placeId())) return "timeline place is missing";
        }
        for (MarginalBinding binding : doc.marginals()) {
            if (!MarginalRegistry.known(binding.behaviorId())) return "unknown marginal " + binding.behaviorId();
        }
        if (edges != null) {
            for (RelationEdge edge : edges) {
                if (edge.fromId().isBlank() || edge.toId().isBlank()) return "relation endpoint is blank";
                if (edge.affinity() < -100 || edge.affinity() > 100) return "affinity out of range";
            }
        }
        return null;
    }
}
