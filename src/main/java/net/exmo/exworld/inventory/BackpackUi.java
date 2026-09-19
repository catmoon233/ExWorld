package net.exmo.exworld.inventory;

/** Client-only page flag. Server always treats every slot as active. */
public final class BackpackUi {
    public static boolean accessories;
    public static long openTime = System.currentTimeMillis();

    private BackpackUi() {}
}
