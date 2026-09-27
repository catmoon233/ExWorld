package net.exmo.lotm.guimi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handbook pathway ids that name the same ladder as guimi_mod.
 * When upstream lotm already owns the sheet, that id stays canonical and the guimi key is only an alias.
 */
public final class GuimiPathways {
    public static final String NAMESPACE = "lotm";

    private static final Map<String, String> CANONICAL_TO_GUIMI = new LinkedHashMap<>();
    private static final Map<String, String> GUIMI_TO_CANONICAL = new LinkedHashMap<>();
    private static final Map<String, String> ALIAS_TO_CANONICAL = new LinkedHashMap<>();

    static {
        link("warrior", "giant");
        link("thief", "error");
        link("apprentice", "door");
        link("sailor", "tyrant");
        link("wizard", "hermit");
        link("spectator", "visionary");
        link("red_priest", "war");
        link("witch", "witch");
        link("sun", "sun");
        link("mother", "mother");
        link("hanged_man", "hanged_man");
        link("fool", "fool");
        link("paragon", "paragon");
        link("white_tower", "white_tower");
        link("death", "death");
        link("darkness", "darkness");
        link("moon", "moon");
        link("abyss", "abyss");
        link("chained", "chained");
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
        if (!guimi.equals(canonical)) ALIAS_TO_CANONICAL.put(guimi, canonical);
    }
}
