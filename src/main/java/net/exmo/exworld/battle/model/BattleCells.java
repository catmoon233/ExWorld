package net.exmo.exworld.battle.model;

import java.util.ArrayList;
import java.util.List;

/** Grid helpers for ring targeting, directional rays and three-cell cleaves. */
public final class BattleCells {
    private static final int[][] RING = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};

    private BattleCells() {}

    public static BattleCell offset(BattleCell origin, int dx, int dz) {
        return new BattleCell(origin.x() + dx, origin.z() + dz, origin.floorY());
    }

    public static boolean adjacentRing(BattleCell origin, BattleCell cell) {
        return origin != null && cell != null && origin.distanceTo(cell) == 1;
    }

    public static int[] direction(BattleCell origin, BattleCell selected) {
        return new int[] {Integer.signum(selected.x() - origin.x()), Integer.signum(selected.z() - origin.z())};
    }

    public static List<BattleCell> ray(BattleCell origin, int dx, int dz, int length) {
        List<BattleCell> cells = new ArrayList<>();
        if (dx == 0 && dz == 0) return cells;
        for (int step = 1; step <= length; step++) cells.add(offset(origin, dx * step, dz * step));
        return cells;
    }

    /** Inclusive of the destination, exclusive of the origin. */
    public static List<BattleCell> chebyshevLine(BattleCell from, BattleCell to) {
        List<BattleCell> cells = new ArrayList<>();
        if (from == null || to == null || from.equals(to)) return cells;
        int x = from.x(), z = from.z();
        while (x != to.x() || z != to.z()) {
            x += Integer.signum(to.x() - x);
            z += Integer.signum(to.z() - z);
            cells.add(new BattleCell(x, z, from.floorY()));
        }
        return cells;
    }

    public static List<BattleCell> cleaveCells(BattleCell origin, BattleCell selected) {
        int index = ringIndex(origin, selected);
        if (index < 0) return List.of();
        int previous = (index + RING.length - 1) % RING.length;
        int next = (index + 1) % RING.length;
        return List.of(selected, offset(origin, RING[previous][0], RING[previous][1]),
                offset(origin, RING[next][0], RING[next][1]));
    }

    private static int ringIndex(BattleCell origin, BattleCell selected) {
        int dx = selected.x() - origin.x(), dz = selected.z() - origin.z();
        for (int i = 0; i < RING.length; i++) if (RING[i][0] == dx && RING[i][1] == dz) return i;
        return -1;
    }
}