package net.exmo.exworld.battle.api;

import java.util.UUID;

public record BattleId(UUID value) {
    public BattleId { if (value == null) throw new IllegalArgumentException("Battle id is required"); }
    public static BattleId create() { return new BattleId(UUID.randomUUID()); }
    public static BattleId parse(String value) { return new BattleId(UUID.fromString(value)); }
    @Override public String toString() { return value.toString(); }
}
