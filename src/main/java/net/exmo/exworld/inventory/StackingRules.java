package net.exmo.exworld.inventory;

/** Count transfer math used when merging identical stacks. */
public final class StackingRules {
    private StackingRules() {}

    public static int transferable(int sourceCount, int destinationCount, int maxCount) {
        if (sourceCount <= 0 || maxCount <= 0) return 0;
        return Math.max(0, Math.min(sourceCount, maxCount - Math.max(0, destinationCount)));
    }
}
