package net.exmo.exworld.client.webview;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Built-in page and the only navigation policy: never open file: or javascript: URLs. */
public final class WebView2Navigation {
    public static final String PAGE = "/assets/exworld/webview/index.html";

    private WebView2Navigation() {}

    public static String builtinHtml() throws IOException {
        try (InputStream in = WebView2Navigation.class.getResourceAsStream(PAGE)) {
            if (in == null) throw new IOException("missing " + PAGE);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static boolean allows(String url) {
        if (url == null) return false;
        String trimmed = url.strip();
        if (trimmed.isEmpty()) return false;
        int colon = trimmed.indexOf(':');
        if (colon <= 0) return false;
        String scheme = trimmed.substring(0, colon).toLowerCase(Locale.ROOT);
        if (scheme.equals("file") || scheme.equals("javascript")) return false;
        return scheme.equals("https") || scheme.equals("http") || scheme.equals("about");
    }

    public static boolean requestsClose(String json) {
        if (json == null) return false;
        String compact = json.replace(" ", "").replace("\n", "").replace("\r", "").replace("\t", "");
        return compact.contains("\"type\":\"close\"") || compact.contains("\\\"type\\\":\\\"close\\\"");
    }

    /** URL from a page message such as {"type":"navigate","url":"..."}. Empty when absent. */
    public static String navigateTarget(String json) {
        if (json == null) return "";
        int key = json.indexOf("\"url\"");
        if (key < 0) return "";
        int colon = json.indexOf(':', key + 5);
        if (colon < 0) return "";
        int quote = json.indexOf('"', colon + 1);
        if (quote < 0) return "";
        int end = json.indexOf('"', quote + 1);
        if (end < 0) return "";
        return json.substring(quote + 1, end);
    }
}
