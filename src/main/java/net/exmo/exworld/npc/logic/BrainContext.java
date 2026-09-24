package net.exmo.exworld.npc.logic;

import net.exmo.exworld.npc.data.ActionSpec;
import net.exmo.exworld.npc.data.MarginalBinding;
import net.exmo.exworld.npc.data.TimelineNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Facts the world tick observed. Missing targets stay false instead of throwing. */
public record BrainContext(
        int minute,
        long tick,
        boolean dialogOpen,
        boolean combatNearby,
        boolean blocked,
        boolean hurt,
        boolean leashedAway,
        boolean knownVisible,
        int knownAffinity,
        boolean pathFailed,
        boolean waypointBlocked,
        boolean interactionNearby,
        int stuck,
        String currentNodeId,
        String suspendedNodeId,
        String activeMarginalId,
        long marginalEndsAt,
        int sequenceIndex,
        List<TimelineNode> nodes,
        List<MarginalBinding> marginals,
        Map<String, ActionSpec> actions,
        Set<String> placeIds,
        Set<String> routeIds,
        String homePlaceId,
        Set<String> knownActionTypes) {
    public BrainContext {
        currentNodeId = currentNodeId == null ? "" : currentNodeId;
        suspendedNodeId = suspendedNodeId == null ? "" : suspendedNodeId;
        activeMarginalId = activeMarginalId == null ? "" : activeMarginalId;
        homePlaceId = homePlaceId == null ? "" : homePlaceId;
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        marginals = marginals == null ? List.of() : List.copyOf(marginals);
        actions = actions == null ? Map.of() : Map.copyOf(actions);
        placeIds = placeIds == null ? Set.of() : Set.copyOf(placeIds);
        routeIds = routeIds == null ? Set.of() : Set.copyOf(routeIds);
        knownActionTypes = knownActionTypes == null ? Set.of() : Set.copyOf(knownActionTypes);
    }

    public BrainContext withMarginals(java.util.List<MarginalBinding> next) {
        return new BrainContext(minute, tick, dialogOpen, combatNearby, blocked, hurt, leashedAway, knownVisible, knownAffinity,
                pathFailed, waypointBlocked, interactionNearby, stuck, currentNodeId, suspendedNodeId, activeMarginalId,
                marginalEndsAt, sequenceIndex, nodes, next, actions, placeIds, routeIds, homePlaceId, knownActionTypes);
    }

    public BrainContext withSuspended(String id) {
        return new BrainContext(minute, tick, dialogOpen, combatNearby, blocked, hurt, leashedAway, knownVisible, knownAffinity,
                pathFailed, waypointBlocked, interactionNearby, stuck, currentNodeId, id, activeMarginalId,
                marginalEndsAt, sequenceIndex, nodes, marginals, actions, placeIds, routeIds, homePlaceId, knownActionTypes);
    }
}
