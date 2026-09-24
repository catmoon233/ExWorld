package net.exmo.exworld.npc.data;

import java.util.Map;

public record MarginalBinding(
        String behaviorId,
        boolean enabled,
        int priority,
        double radius,
        int durationTicks,
        Map<String, String> params) {
    public MarginalBinding {
        behaviorId = behaviorId == null ? "" : behaviorId.trim();
        radius = radius <= 0 ? 8 : radius;
        durationTicks = Math.max(1, durationTicks);
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public int paramInt(String key, int fallback) {
        try {
            String value = params.get(key);
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
