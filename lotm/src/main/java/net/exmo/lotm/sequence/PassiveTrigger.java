package net.exmo.lotm.sequence;

public enum PassiveTrigger {
    HURT,
    ATTACK,
    TICK,
    KILL;

    public static PassiveTrigger parse(String raw) {
        if (raw == null) return TICK;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return TICK;
        }
    }
}
