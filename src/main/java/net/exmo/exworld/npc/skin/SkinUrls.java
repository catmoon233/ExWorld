package net.exmo.exworld.npc.skin;

import java.net.InetAddress;
import java.net.URI;

/** Shared checks for remote NPC skins. Private and loopback hosts are rejected. */
public final class SkinUrls {
    private SkinUrls() {}

    public static boolean http(String raw) {
        return raw != null && (raw.startsWith("http://") || raw.startsWith("https://"));
    }

    public static String reject(String raw) {
        if (raw == null || raw.isBlank()) return "blank";
        URI uri;
        try {
            uri = URI.create(raw.trim());
        } catch (RuntimeException ex) {
            return "bad-url";
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) return "scheme";
        if (uri.getUserInfo() != null) return "userinfo";
        String host = uri.getHost();
        if (host == null || host.isBlank()) return "host";
        String lower = host.toLowerCase();
        if (lower.equals("localhost") || lower.endsWith(".local") || lower.endsWith(".localhost")) return "local";
        return null;
    }

    public static boolean privateAddress(InetAddress address) {
        if (address == null) return true;
        return address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress();
    }
}