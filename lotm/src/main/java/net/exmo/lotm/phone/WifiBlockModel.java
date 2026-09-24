package net.exmo.lotm.phone;

import java.util.UUID;

/**
 * Placed WiFi block only. This type has no phone brand, color, SIM number, or handset fields.
 * Field names and value sets are not shared with {@link PhoneModel}.
 */
public record WifiBlockModel(String hotspotName, String accessSecret, int coverageRadius, String placedBy) {
    public static final int NAME_MIN = 1;
    public static final int NAME_MAX = 24;
    public static final int SECRET_MIN = 0;
    public static final int SECRET_MAX = 32;
    public static final int RADIUS_MIN = 4;
    public static final int RADIUS_MAX = 64;

    public WifiBlockModel {
        hotspotName = clip(hotspotName, NAME_MAX);
        if (hotspotName.length() < NAME_MIN) {
            throw new IllegalArgumentException("hotspotName length must be 1.." + NAME_MAX);
        }
        accessSecret = clip(accessSecret, SECRET_MAX);
        if (accessSecret.length() < SECRET_MIN || accessSecret.length() > SECRET_MAX) {
            throw new IllegalArgumentException("accessSecret length must be 0.." + SECRET_MAX);
        }
        if (coverageRadius < RADIUS_MIN || coverageRadius > RADIUS_MAX) {
            throw new IllegalArgumentException("coverageRadius must be " + RADIUS_MIN + ".." + RADIUS_MAX);
        }
        placedBy = placedBy == null ? "" : placedBy.trim();
        if (!placedBy.isEmpty()) {
            try {
                UUID.fromString(placedBy);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("placedBy must be empty or a UUID");
            }
        }
    }

    /** Rejects out-of-range radius. Blank name is rejected rather than replaced. */
    public static WifiBlockModel require(String hotspotName, String accessSecret, int coverageRadius, String placedBy) {
        return new WifiBlockModel(hotspotName, accessSecret, coverageRadius, placedBy);
    }

    /** Load path: blank name becomes "WiFi", radius is clamped into 4..64, invalid owner is cleared. */
    public static WifiBlockModel normalize(String hotspotName, String accessSecret, int coverageRadius, String placedBy) {
        String name = clip(hotspotName, NAME_MAX);
        if (name.isBlank()) name = "WiFi";
        int radius = Math.max(RADIUS_MIN, Math.min(RADIUS_MAX, coverageRadius));
        String owner = placedBy == null ? "" : placedBy.trim();
        try {
            if (!owner.isEmpty()) UUID.fromString(owner);
        } catch (IllegalArgumentException ex) {
            owner = "";
        }
        return new WifiBlockModel(name, accessSecret, radius, owner);
    }

    private static String clip(String text, int max) {
        String value = text == null ? "" : text.replace('\n', ' ').replace('\t', ' ').trim();
        return value.length() <= max ? value : value.substring(0, max);
    }
}
