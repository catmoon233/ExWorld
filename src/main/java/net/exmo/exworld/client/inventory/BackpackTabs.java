package net.exmo.exworld.client.inventory;

/** Optional backpack tabs. ExWorld owns the character sheet; sequence is contributed by another mod. */
public final class BackpackTabs {
    public static final int TAB_W = 60;
    public static final int TAB_STEP = 64;
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

    public static int sequenceTabX(int panelX) {
        return hasSequence() ? panelX + TAB_STEP : -1;
    }

    public static int characterTabX(int panelX) {
        return panelX + (hasSequence() ? TAB_STEP * 2 : TAB_STEP);
    }
}
