package net.exmo.exworld.battle.arena;

import net.exmo.exworld.battle.model.BattleCell;

import java.util.Map;
import java.util.Set;

public record ArenaDefinition(String id, int width, int depth, int baseY, int maxStepHeight,
                              Map<GridPoint, Integer> floorHeights, Set<GridPoint> blocked,
                              Map<String, Set<GridPoint>> deploymentZones) {
    public ArenaDefinition {
        if (width <= 0 || depth <= 0) throw new IllegalArgumentException("Arena dimensions must be positive");
        floorHeights = Map.copyOf(floorHeights);
        blocked = Set.copyOf(blocked);
        deploymentZones = Map.copyOf(deploymentZones);
    }

    public static ArenaDefinition flat(String id, int size, int baseY) {
        return new ArenaDefinition(id, size, size, baseY, 1, Map.of(), Set.of(), Map.of());
    }

    public boolean contains(GridPoint point) { return point.x >= 0 && point.x < width && point.z >= 0 && point.z < depth; }
    public int floorAt(GridPoint point) { return floorHeights.getOrDefault(point, baseY); }
    public BattleCell cell(GridPoint point) { return new BattleCell(point.x, point.z, floorAt(point)); }
    public record GridPoint(int x, int z) {}
}
