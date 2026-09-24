package net.exmo.exworld.npc.data;

import java.util.List;
import java.util.Optional;

/** Editable urban NPC document. Entities store only the id and runtime state. */
public record NpcDocument(
        String id,
        String displayName,
        String texture,
        String homePlaceId,
        String activeRouteId,
        String defaultDialogId,
        boolean anchored,
        List<NpcPlace> places,
        List<NpcRoute> routes,
        List<ActionSpec> actions,
        List<TimelineNode> timeline,
        List<DialogScript> dialogs,
        List<MarginalBinding> marginals,
        List<TradeSpec> trades,
        AiSettings ai,
        NpcLoadout loadout) {
    public NpcDocument {
        id = id == null ? "" : id.trim();
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        texture = texture == null ? "" : texture.trim();
        homePlaceId = homePlaceId == null ? "" : homePlaceId.trim();
        activeRouteId = activeRouteId == null ? "" : activeRouteId.trim();
        defaultDialogId = defaultDialogId == null ? "" : defaultDialogId.trim();
        places = places == null ? List.of() : List.copyOf(places);
        routes = routes == null ? List.of() : List.copyOf(routes);
        actions = actions == null ? List.of() : List.copyOf(actions);
        timeline = timeline == null ? List.of() : List.copyOf(timeline);
        dialogs = dialogs == null ? List.of() : List.copyOf(dialogs);
        marginals = marginals == null ? List.of() : List.copyOf(marginals);
        trades = trades == null ? List.of() : List.copyOf(trades);
        ai = ai == null ? AiSettings.DEFAULT : ai;
        loadout = loadout == null ? NpcLoadout.EMPTY : loadout;
    }

    public Optional<NpcPlace> place(String placeId) {
        if (placeId == null || placeId.isBlank()) return Optional.empty();
        return places.stream().filter(place -> place.id().equals(placeId)).findFirst();
    }

    public Optional<NpcRoute> route(String routeId) {
        if (routeId == null || routeId.isBlank()) return Optional.empty();
        return routes.stream().filter(route -> route.id().equals(routeId)).findFirst();
    }

    public Optional<ActionSpec> action(String actionId) {
        if (actionId == null || actionId.isBlank()) return Optional.empty();
        return actions.stream().filter(action -> action.id().equals(actionId)).findFirst();
    }

    public Optional<DialogScript> dialog(String dialogId) {
        if (dialogId == null || dialogId.isBlank()) return Optional.empty();
        return dialogs.stream().filter(dialog -> dialog.id().equals(dialogId)).findFirst();
    }

    public Optional<MarginalBinding> marginal(String behaviorId) {
        if (behaviorId == null || behaviorId.isBlank()) return Optional.empty();
        return marginals.stream().filter(binding -> binding.behaviorId().equals(behaviorId)).findFirst();
    }

    public NpcDocument withPlaces(List<NpcPlace> next, boolean nextAnchored) {
        return new NpcDocument(id, displayName, texture, homePlaceId, activeRouteId, defaultDialogId, nextAnchored,
                next, routes, actions, timeline, dialogs, marginals, trades, ai, loadout);
    }

    public NpcDocument withRoutes(List<NpcRoute> next) {
        return new NpcDocument(id, displayName, texture, homePlaceId, activeRouteId, defaultDialogId, anchored,
                places, next, actions, timeline, dialogs, marginals, trades, ai, loadout);
    }

    public NpcDocument withHome(String placeId) {
        return new NpcDocument(id, displayName, texture, placeId, activeRouteId, defaultDialogId, anchored,
                places, routes, actions, timeline, dialogs, marginals, trades, ai, loadout);
    }

    public NpcDocument withLoadout(NpcLoadout next) {
        return new NpcDocument(id, displayName, texture, homePlaceId, activeRouteId, defaultDialogId, anchored,
                places, routes, actions, timeline, dialogs, marginals, trades, ai, next);
    }
 
     public NpcDocument withId(String newId, boolean nextAnchored) {
         return new NpcDocument(newId, displayName, texture, homePlaceId, activeRouteId, defaultDialogId, nextAnchored,
                 places, routes, actions, timeline, dialogs, marginals, trades, ai, loadout);
     }
 
     public NpcDocument withActiveRoute(String routeId) {
         return new NpcDocument(id, displayName, texture, homePlaceId, routeId == null ? "" : routeId.trim(), defaultDialogId, anchored,
                 places, routes, actions, timeline, dialogs, marginals, trades, ai, loadout);
     }
}