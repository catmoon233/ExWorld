package net.exmo.exworld.battle.compat;

/** Converts Iron's effective cast duration into the authoritative battle action lock. */
public final class IronSpellTiming {
    private IronSpellTiming() {}

    public static int actionTicks(int effectiveCastTime) {
        return Math.max(1, effectiveCastTime);
    }
}
