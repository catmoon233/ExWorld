package net.exmo.exphone;

/** Converts Minecraft day time into a clock and a calendar date. Day 0 is year 1, month 1, day 1. */
final class PhoneClock {
    private static final String WEEK = "一二三四五六日";

    private PhoneClock() {}

    static String time(long dayTime) {
        long ticks = Math.floorMod(dayTime, 24000L);
        int hours = (int) ((ticks / 1000L + 6) % 24);
        int minutes = (int) ((ticks % 1000L) * 60L / 1000L);
        return new String(new char[]{
                (char) ('0' + hours / 10),
                (char) ('0' + hours % 10),
                ':',
                (char) ('0' + minutes / 10),
                (char) ('0' + minutes % 10)
        });
    }

    static String date(long dayTime) {
        long day = Math.max(0L, Math.floorDiv(dayTime, 24000L));
        int year = (int) (day / 360L) + 1;
        int month = (int) ((day % 360L) / 30L) + 1;
        int date = (int) (day % 30L) + 1;
        return year + "年" + month + "月" + date + "日 周" + WEEK.charAt((int) (day % 7));
    }
}
