package net.exmo.exworld.npc.data;

import java.util.List;

/** A route is either pathfound destinations or an exact polyline. Points are never rewritten by movement. */
public record NpcRoute(String id, RouteMode mode, boolean loop, double speed, List<Waypoint> points) {
    public NpcRoute {
        id = id == null ? "" : id.trim();
        mode = mode == null ? RouteMode.PATHFIND : mode;
        speed = speed <= 0 ? 1.0 : Math.min(speed, 4.0);
        points = points == null ? List.of() : List.copyOf(points);
    }

    public record Waypoint(String placeId, double x, double y, double z) {
        public Waypoint {
            placeId = placeId == null ? "" : placeId.trim();
        }
    }
 
     public NpcRoute appendPlace(String placeId) {
         if (placeId == null || placeId.isBlank()) return this;
         java.util.ArrayList<Waypoint> next = new java.util.ArrayList<>(points);
         if (!next.isEmpty() && placeId.equals(next.get(next.size() - 1).placeId())) return this;
         next.add(new Waypoint(placeId, 0, 0, 0));
         return new NpcRoute(id, mode, loop, speed, next);
     }
 
     public NpcRoute dropLast() {
         if (points.isEmpty()) return this;
         java.util.ArrayList<Waypoint> next = new java.util.ArrayList<>(points);
         next.remove(next.size() - 1);
         return new NpcRoute(id, mode, loop, speed, next);
     }
}
