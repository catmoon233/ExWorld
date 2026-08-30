package net.exmo.exworld.battle.model;

public record BattleCell(int x, int z, int floorY) {
    public int distanceTo(BattleCell other) { return Math.max(Math.abs(x - other.x), Math.abs(z - other.z)); }
    public boolean withinRadius(BattleCell other, int radius) {
        long dx = x - other.x, dz = z - other.z;
        return dx * dx + dz * dz <= (long) radius * radius;
    }
    public BattleCell atHeight(int y) { return new BattleCell(x, z, y); }
}
