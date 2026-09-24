package net.exmo.exworld.client.webview;

import java.util.Locale;

/** WebView2 ships a loader and Runtime for Windows x64 only. */
public final class WebView2Platform {
    private WebView2Platform() {}

    public static boolean supported() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return os.contains("windows") && (arch.equals("amd64") || arch.equals("x86_64"));
    }
}
