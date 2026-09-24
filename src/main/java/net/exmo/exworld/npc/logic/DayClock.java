package net.exmo.exworld.npc.logic;

/** Converts the vanilla day cycle into an editable minute of the day. */
public final class DayClock {
    private DayClock() {}

    public static int minuteOfDay(long dayTime) {
        long ticks = Math.floorMod(dayTime, 24000L);
        int minute = (int) (ticks * 1449L / 24000L);
        if (minute < 0) return 0;
        return Math.min(minute, 1439);
    }
}
