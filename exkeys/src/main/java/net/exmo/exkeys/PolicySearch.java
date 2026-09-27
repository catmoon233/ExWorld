package net.exmo.exkeys;

import java.util.Locale;

public final class PolicySearch {
    private PolicySearch() {}

    public static boolean matches(String query, String id, String name, String category, String bound) {
        if (query == null || query.isBlank()) return true;
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return contains(id, needle) || contains(name, needle) || contains(category, needle) || contains(bound, needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
