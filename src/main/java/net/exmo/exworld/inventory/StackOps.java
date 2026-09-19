package net.exmo.exworld.inventory;

/** Platform-neutral stack operations used by admission and merging. */
public interface StackOps<T> {
    boolean isEmpty(T stack);

    boolean sameIgnoringRotation(T left, T right);

    int count(T stack);

    int maxCount(T stack);

    T withCount(T stack, int count);

    ItemFootprint footprint(T stack);

    T withRotated(T stack, boolean rotated);

    boolean rotated(T stack);
}
