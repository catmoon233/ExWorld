package net.exmo.lotm.sequence;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * ExWorld pathway ids that name the same ladder as guimi_mod.
 * Canonical ids keep the handbook names when they differ from the guimi key.
 */
public final class GuimiPathways {
    public static final String NAMESPACE = "lotm";

    private static final Map<String, String> CANONICAL_TO_GUIMI = new LinkedHashMap<>();
    private static final Map<String, String> GUIMI_TO_CANONICAL = new LinkedHashMap<>();
    private static final Map<String, String> ALIAS_TO_CANONICAL = new LinkedHashMap<>();

    static {
        link("fool", "fool");
        link("error", "error");
        alias("thief", "error");
        link("door", "door");
        alias("apprentice", "door");
        link("paragon", "paragon");
        link("hanged_man", "hanged_man");
        link("sun", "sun");
        link("tyrant", "tyrant");
        alias("sailor", "tyrant");
        link("white_tower", "white_tower");
        link("visionary", "visionary");
        alias("spectator", "visionary");
        link("death", "death");
        link("darkness", "darkness");
        link("warrior", "giant");
        link("red_priest", "war");
        link("wizard", "hermit");
        link("moon", "moon");
        link("mother", "mother");
        link("abyss", "abyss");
        link("chained", "chained");
        link("witch", "witch");
        link("justice", "justice");
        link("black_emperor", "black_emperor");
        link("wheel", "wheel");
    }

    private GuimiPathways() {}

    public static String canonicalPath(String guimiKey) {
        if (guimiKey == null || guimiKey.isBlank()) return "";
        return GUIMI_TO_CANONICAL.getOrDefault(guimiKey, guimiKey);
    }

    public static Optional<String> guimiKeyOfCanonical(String canonicalPath) {
        if (canonicalPath == null) return Optional.empty();
        return Optional.ofNullable(CANONICAL_TO_GUIMI.get(canonicalPath));
    }

    public static Optional<String> resolveCanonical(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String path = pathOf(raw.trim());
        if (CANONICAL_TO_GUIMI.containsKey(path)) return Optional.of(path);
        String aliased = ALIAS_TO_CANONICAL.get(path);
        if (aliased != null) return Optional.of(aliased);
        if (GUIMI_TO_CANONICAL.containsKey(path)) return Optional.of(GUIMI_TO_CANONICAL.get(path));
        return Optional.empty();
    }

    public static Optional<String> guimiKey(String raw) {
        return resolveCanonical(raw).flatMap(GuimiPathways::guimiKeyOfCanonical);
    }

    public static Map<String, String> aliases() {
        return Map.copyOf(ALIAS_TO_CANONICAL);
    }

    public static Map<String, String> canonicalToGuimi() {
        return Map.copyOf(CANONICAL_TO_GUIMI);
    }

    private static String pathOf(String token) {
        int colon = token.indexOf(':');
        return colon >= 0 ? token.substring(colon + 1) : token;
    }

    private static void link(String canonical, String guimi) {
        CANONICAL_TO_GUIMI.put(canonical, guimi);
        GUIMI_TO_CANONICAL.put(guimi, canonical);
        alias(guimi, canonical);
    }

    private static void alias(String alias, String canonical) {
        if (!alias.equals(canonical)) ALIAS_TO_CANONICAL.put(alias, canonical);
    }
}
