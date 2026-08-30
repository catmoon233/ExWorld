package net.exmo.exworld.battle.arena;

import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.BattlePathDirections;

import java.util.*;
import java.util.function.Predicate;

/** 2.5D navigation, reservation and line-of-sight module. */
public final class ArenaGrid {
    private final ArenaDefinition definition;
    private final Map<ArenaDefinition.GridPoint, UUID> occupants = new HashMap<>();
    private final Map<ArenaDefinition.GridPoint, UUID> reservations = new HashMap<>();

    public ArenaGrid(ArenaDefinition definition) { this.definition = definition; }
    public ArenaDefinition definition() { return definition; }

    public boolean place(UUID combatant, BattleCell cell) {
        var point = point(cell);
        if (!walkable(point, combatant) || occupants.containsKey(point)) return false;
        occupants.entrySet().removeIf(entry -> entry.getValue().equals(combatant));
        occupants.put(point, combatant);
        return true;
    }

    public Optional<List<BattleCell>> reservePath(UUID combatant, BattleCell from, BattleCell to, int budget) {
        var start = point(from);
        var goal = point(to);
        if (!walkable(start, combatant) || !walkable(goal, combatant)) return Optional.empty();
        record Step(ArenaDefinition.GridPoint point, int cost) {}
        ArrayDeque<Step> queue = new ArrayDeque<>();
        Map<ArenaDefinition.GridPoint, ArenaDefinition.GridPoint> previous = new HashMap<>();
        Map<ArenaDefinition.GridPoint, Integer> cost = new HashMap<>();
        queue.add(new Step(start, 0)); cost.put(start, 0);
        while (!queue.isEmpty()) {
            Step current = queue.removeFirst();
            if (current.point.equals(goal)) break;
            for (int[] direction : BattlePathDirections.toward(current.point.x(), current.point.z(), goal.x(), goal.z())) {
                var next = new ArenaDefinition.GridPoint(current.point.x() + direction[0], current.point.z() + direction[1]);
                int nextCost = current.cost + 1;
                if (nextCost > budget || cost.containsKey(next) || !walkable(next, combatant)) continue;
                if (direction[0] != 0 && direction[1] != 0 && cornerBlocked(current.point, direction[0], direction[1], combatant)) continue;
                previous.put(next, current.point); cost.put(next, nextCost); queue.addLast(new Step(next, nextCost));
            }
        }
        if (!cost.containsKey(goal)) return Optional.empty();
        List<BattleCell> path = new ArrayList<>();
        for (var cursor = goal; !cursor.equals(start); cursor = previous.get(cursor)) path.add(definition.cell(cursor));
        Collections.reverse(path);
        if (path.stream().map(ArenaGrid::point).anyMatch(cell -> reservations.containsKey(cell)
                && !reservations.get(cell).equals(combatant))) return Optional.empty();
        reservations.entrySet().removeIf(entry -> entry.getValue().equals(combatant));
        path.forEach(cell -> reservations.put(point(cell), combatant));
        return Optional.of(List.copyOf(path));
    }

    public boolean reserveDestination(UUID combatant, BattleCell destination) {
        ArenaDefinition.GridPoint point = point(destination);
        if (!walkable(point, combatant)) return false;
        release(combatant); reservations.put(point, combatant); return true;
    }

    public void commit(UUID combatant, BattleCell destination) {
        occupants.entrySet().removeIf(entry -> entry.getValue().equals(combatant));
        occupants.put(point(destination), combatant);
        release(combatant);
    }

    public void release(UUID combatant) { reservations.entrySet().removeIf(entry -> entry.getValue().equals(combatant)); }
    public void restoreReservation(UUID combatant, Collection<BattleCell> path) {
        release(combatant);
        for (BattleCell cell : path) {
            ArenaDefinition.GridPoint point = point(cell);
            UUID occupied = occupants.get(point), reserved = reservations.get(point);
            if (!definition.contains(point) || definition.blocked().contains(point)
                    || occupied != null && !occupied.equals(combatant) || reserved != null && !reserved.equals(combatant))
                throw new IllegalArgumentException("Cannot restore movement reservation at " + cell);
            reservations.put(point, combatant);
        }
    }
    public void remove(UUID combatant) { occupants.entrySet().removeIf(entry -> entry.getValue().equals(combatant)); release(combatant); }
    public Optional<UUID> occupant(BattleCell cell) { return Optional.ofNullable(occupants.get(point(cell))); }

    public boolean hasLineOfSight(BattleCell from, BattleCell to, UUID actor, UUID target,
                                  boolean piercesUnits, boolean ignoresTerrain) {
        return hasLineOfSight(from, to, actor, target, piercesUnits, ignoresTerrain, ignored -> true);
    }

    /** Tactical occupancy and ranged occlusion are separate: elevated units retain a cell without blocking a ray. */
    public boolean hasLineOfSight(BattleCell from, BattleCell to, UUID actor, UUID target,
                                  boolean piercesUnits, boolean ignoresTerrain, Predicate<UUID> blocksRangedLineOfSight) {
        int x0 = from.x(), z0 = from.z(), x1 = to.x(), z1 = to.z();
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dz = -Math.abs(z1 - z0), sz = z0 < z1 ? 1 : -1;
        int error = dx + dz;
        while (true) {
            var point = new ArenaDefinition.GridPoint(x0, z0);
            boolean endpoint = x0 == x1 && z0 == z1;
            if (!(x0 == from.x() && z0 == from.z())) {
                if (!ignoresTerrain && definition.blocked().contains(point)) return false;
                UUID occupant = occupants.get(point);
                if (!piercesUnits && occupant != null && !occupant.equals(actor) && !(endpoint && occupant.equals(target))
                        && blocksRangedLineOfSight.test(occupant)) return false;
            }
            if (endpoint) return true;
            int doubled = 2 * error;
            if (doubled >= dz) { error += dz; x0 += sx; }
            if (doubled <= dx) { error += dx; z0 += sz; }
        }
    }

    private boolean cornerBlocked(ArenaDefinition.GridPoint from, int dx, int dz, UUID actor) {
        return !walkable(new ArenaDefinition.GridPoint(from.x() + dx, from.z()), actor)
                && !walkable(new ArenaDefinition.GridPoint(from.x(), from.z() + dz), actor);
    }

    private boolean walkable(ArenaDefinition.GridPoint point, UUID actor) {
        if (!definition.contains(point) || definition.blocked().contains(point)) return false;
        UUID occupied = occupants.get(point);
        if (occupied != null && !occupied.equals(actor)) return false;
        UUID reserved = reservations.get(point);
        return reserved == null || reserved.equals(actor);
    }

    private static ArenaDefinition.GridPoint point(BattleCell cell) { return new ArenaDefinition.GridPoint(cell.x(), cell.z()); }
}
