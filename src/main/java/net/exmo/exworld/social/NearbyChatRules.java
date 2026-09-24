package net.exmo.exworld.social;

/** Pure audience rules for adventure proximity chat and spectator speech. */
public final class NearbyChatRules {
    public enum Audience { ADVENTURE, SPECTATOR, OTHER }

    private NearbyChatRules() {}

    public static Audience audience(boolean spectator, boolean adventure) {
        if (spectator) return Audience.SPECTATOR;
        return adventure ? Audience.ADVENTURE : Audience.OTHER;
    }

    /** Adventure and spectator chat are not vanilla global broadcasts. */
    public static boolean rewrites(Audience speaker) {
        return speaker == Audience.ADVENTURE || speaker == Audience.SPECTATOR;
    }

    /**
     * Adventure speech is heard only inside the configured horizontal distance and vertical range.
     * Spectator speech is heard by everyone except adventure players, at any distance.
     */
    public static boolean hears(Audience listener, Audience speaker, boolean sameDimension,
                                double dx, double dy, double dz, double distance, double range) {
        if (speaker == Audience.SPECTATOR) return listener != Audience.ADVENTURE;
        if (speaker != Audience.ADVENTURE) return true;
        if (!sameDimension || distance < 0.0 || range < 0.0) return false;
        double horizontalSq = dx * dx + dz * dz;
        return horizontalSq <= distance * distance && Math.abs(dy) <= range;
    }
}
