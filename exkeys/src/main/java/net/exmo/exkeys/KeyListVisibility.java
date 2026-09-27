package net.exmo.exkeys;

/** Drops hidden key rows and the category headers that would be left empty. */
public final class KeyListVisibility {
    private KeyListVisibility() {}

    public static boolean[] keep(boolean[] category, boolean[] hidden) {
        if (category == null || hidden == null || category.length != hidden.length) {
            throw new IllegalArgumentException("category and hidden must be the same length");
        }
        boolean[] keep = new boolean[category.length];
        for (int i = 0; i < category.length; i++) {
            if (!category[i]) keep[i] = !hidden[i];
        }
        for (int i = 0; i < category.length; i++) {
            if (!category[i]) continue;
            boolean any = false;
            for (int next = i + 1; next < category.length && !category[next]; next++) {
                if (keep[next]) any = true;
            }
            keep[i] = any;
        }
        return keep;
    }
}
