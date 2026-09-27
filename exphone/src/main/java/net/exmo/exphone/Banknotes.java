package net.exmo.exphone;

/** Paper gold. Values are the face value in account gold. */
public final class Banknotes {
    public static final int[] VALUES = {1, 5, 10, 20, 50, 100};
    public static final int MAX_COUNT = 36 * 64;

    private Banknotes() {}

    public static boolean valid(int value) {
        for (int face : VALUES) if (face == value) return true;
        return false;
    }

    /** Face value times count, or -1 when the request cannot become items. */
    public static long cost(int value, int count) {
        if (!valid(value) || count <= 0 || count > MAX_COUNT) return -1;
        return (long) value * count;
    }
}
