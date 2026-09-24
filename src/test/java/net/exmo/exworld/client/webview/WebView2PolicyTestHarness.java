package net.exmo.exworld.client.webview;

public final class WebView2PolicyTestHarness {
    public static void main(String[] args) throws Exception {
        WebView2Requests disabled = new WebView2Requests(false);
        disabled.open();
        disabled.close();
        disabled.post("{\"type\":\"ping\"}");
        check(disabled.ignored() == 3 && disabled.drain().isEmpty(), "non-windows request queue is a no-op");

        WebView2Requests enabled = new WebView2Requests(true);
        enabled.open();
        enabled.post("{\"type\":\"close\"}");
        var queued = enabled.drain();
        check(enabled.ignored() == 0 && queued.size() == 2 && queued.getFirst().kind() == WebView2Requests.Kind.OPEN,
                "supported platforms keep the request queue");

        String html = WebView2Navigation.builtinHtml();
        check(html.contains("桥接正常") && html.contains("window.exworld") && html.contains("关闭"),
                "builtin html loads from the classpath");
        check(!html.toLowerCase().contains("file:"), "builtin html does not point at a local file");
        check(!WebView2Navigation.allows("file:///C:/secret.html"), "rejects file urls");
        check(!WebView2Navigation.allows("FILE:///C:/secret.html"), "rejects file urls regardless of case");
        check(!WebView2Navigation.allows("javascript:alert(1)"), "rejects javascript urls");
        check(!WebView2Navigation.allows("  JavaScript:alert(1)"), "rejects javascript urls with spacing and case");
        check(!WebView2Navigation.allows("C:\\Windows\\secret.txt"), "rejects bare local paths");
        check(WebView2Navigation.allows("about:blank"), "about:blank remains available for the initial document");
        check(WebView2Navigation.requestsClose("{\"type\":\"close\"}"), "page close messages are recognized");
        check(!WebView2Navigation.allows(WebView2Navigation.navigateTarget("{\"type\":\"navigate\",\"url\":\"javascript:alert(1)\"}")),
                "a navigate message cannot carry a javascript url");
        byte[] iid = WebView2Com.iid("4e8a3389-c9d8-4bd2-b6b5-124fee6cc14d");
        check(iid[0] == (byte) 0x89 && iid[1] == (byte) 0x33 && iid[2] == (byte) 0x8a && iid[3] == (byte) 0x4e,
                "environment handler iid is little-endian");
        System.out.println("webview2 policy ok");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
