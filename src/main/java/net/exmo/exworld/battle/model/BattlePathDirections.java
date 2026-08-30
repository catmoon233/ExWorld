package net.exmo.exworld.battle.model;

import java.util.Arrays;

/** Orders equal-cost grid steps by how directly they approach the requested destination. */
public final class BattlePathDirections {
    private static final int[][] DIRECTIONS = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};

    private BattlePathDirections() {}

    public static int[][] toward(int fromX, int fromZ, int goalX, int goalZ) {
        int[][] ordered = DIRECTIONS.clone();
        Arrays.sort(ordered, (left, right) -> Long.compare(
                distanceSquared(fromX + left[0], fromZ + left[1], goalX, goalZ),
                distanceSquared(fromX + right[0], fromZ + right[1], goalX, goalZ)));
        return ordered;
    }

    private static long distanceSquared(int x, int z, int goalX, int goalZ) {
        long dx = goalX - x, dz = goalZ - z;
        return dx * dx + dz * dz;
    }
}
