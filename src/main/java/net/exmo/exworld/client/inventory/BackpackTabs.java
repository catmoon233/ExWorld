package net.exmo.exworld.client.inventory;

/** Optional backpack tab contributed by another mod. ExWorld does not own sequence content. */
public final class BackpackTabs {
    private static Runnable sequenceOpen;

    private BackpackTabs() {}

    public static void setSequenceOpen(Runnable opener) {
        sequenceOpen = opener;
    }

    public static boolean hasSequence() {
        return sequenceOpen != null;
    }

    public static void openSequence() {
        if (sequenceOpen != null) sequenceOpen.run();
    }
}
