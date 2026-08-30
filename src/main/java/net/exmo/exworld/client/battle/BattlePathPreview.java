package net.exmo.exworld.client.battle;

import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.BattlePathDirections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Client-side mirror of the grid path search, used only to preview the authoritative move request. */
public final class BattlePathPreview {
    private BattlePathPreview() {}

    public static Optional<List<BattleCell>> findPath(int arenaSize, BattleCell start, BattleCell destination, int budget,
                                                       Collection<BattleCell> unavailableCells) {
        if (arenaSize <= 0 || start == null || destination == null || budget < 0) return Optional.empty();
        Point source = Point.of(start), goal = Point.of(destination);
        Set<Point> unavailable = new HashSet<>();
        unavailableCells.forEach(cell -> unavailable.add(Point.of(cell)));
        unavailable.remove(source);
        if (!inside(source, arenaSize) || !inside(goal, arenaSize) || unavailable.contains(goal)) return Optional.empty();

        ArrayDeque<Step> queue = new ArrayDeque<>();
        Map<Point, Point> previous = new HashMap<>();
        Map<Point, Integer> costs = new HashMap<>();
        queue.addLast(new Step(source, 0)); costs.put(source, 0);
        while (!queue.isEmpty()) {
            Step current = queue.removeFirst();
            if (current.point().equals(goal)) break;
            for (int[] direction : BattlePathDirections.toward(current.point().x(), current.point().z(), goal.x(), goal.z())) {
                Point next = new Point(current.point().x() + direction[0], current.point().z() + direction[1]);
                int nextCost = current.cost() + 1;
                if (nextCost > budget || costs.containsKey(next) || !walkable(next, arenaSize, unavailable)) continue;
                if (direction[0] != 0 && direction[1] != 0
                        && cornerBlocked(current.point(), direction[0], direction[1], arenaSize, unavailable)) continue;
                previous.put(next, current.point()); costs.put(next, nextCost); queue.addLast(new Step(next, nextCost));
            }
        }
        if (!costs.containsKey(goal)) return Optional.empty();
        List<BattleCell> path = new ArrayList<>();
        for (Point cursor = goal; !cursor.equals(source); cursor = previous.get(cursor)) {
            path.add(cursor.equals(goal) ? destination : new BattleCell(cursor.x(), cursor.z(), start.floorY()));
        }
        Collections.reverse(path);
        return Optional.of(List.copyOf(path));
    }

    private static boolean cornerBlocked(Point from, int dx, int dz, int arenaSize, Set<Point> unavailable) {
        return !walkable(new Point(from.x() + dx, from.z()), arenaSize, unavailable)
                && !walkable(new Point(from.x(), from.z() + dz), arenaSize, unavailable);
    }

    private static boolean walkable(Point point, int arenaSize, Set<Point> unavailable) {
        return inside(point, arenaSize) && !unavailable.contains(point);
    }

    private static boolean inside(Point point, int arenaSize) {
        return point.x() >= 0 && point.z() >= 0 && point.x() < arenaSize && point.z() < arenaSize;
    }

    private record Point(int x, int z) {
        private static Point of(BattleCell cell) { return new Point(cell.x(), cell.z()); }
    }
    private record Step(Point point, int cost) {}
}
