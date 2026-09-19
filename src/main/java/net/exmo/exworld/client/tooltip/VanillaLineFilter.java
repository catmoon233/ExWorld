package net.exmo.exworld.client.tooltip;

import java.util.ArrayList;
import java.util.List;

/**
 * Drops the vanilla title line, blank separators, and ExModifier's default dump keys
 * so the themed panel can re-present that data as chips and sections.
 */
public final class VanillaLineFilter {
    public static final String EXMODIFIER_KEY_PREFIX = "tooltip.exmodifier.";

    private VanillaLineFilter() {}

    public record Line(String translationKey, String text) {
        public Line {
            translationKey = translationKey == null ? "" : translationKey;
            text = text == null ? "" : text;
        }

        public boolean blank() {
            return text.isBlank();
        }

        public boolean exModifierDump() {
            return translationKey.startsWith(EXMODIFIER_KEY_PREFIX);
        }
    }

    public static List<Line> body(List<Line> lines) {
        if (lines == null || lines.size() <= 1) return List.of();
        List<Line> body = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            Line line = lines.get(i);
            if (line.blank() || line.exModifierDump()) continue;
            body.add(line);
        }
        return List.copyOf(body);
    }
}
