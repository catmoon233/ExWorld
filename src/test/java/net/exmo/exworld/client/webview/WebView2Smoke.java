package net.exmo.exworld.client.webview;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Manual check for the installed WebView2 Runtime. Pass --window to open the built-in page briefly. */
public final class WebView2Smoke {
    public static void main(String[] args) throws Exception {
        Path dll = Files.createTempFile("WebView2Loader", ".dll");
        dll.toFile().deleteOnExit();
        try (InputStream in = WebView2Smoke.class.getResourceAsStream("/natives/windows-x64/WebView2Loader.dll")) {
            if (in == null) throw new IllegalStateException("loader dll is not on the classpath");
            Files.write(dll, in.readAllBytes());
        }
        String version = WebView2Host.availableVersion(dll);
        if (version == null) throw new IllegalStateException("WebView2 Runtime is not installed");
        System.out.println("runtime " + version);
        if (args.length == 0 || !"--window".equals(args[0])) return;

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> failure = new AtomicReference<>();
        WebView2Host.setLog(System.out::println);
        Path userData = Path.of(System.getProperty("java.io.tmpdir"), "exworld-webview2-smoke");
        WebView2Host.open(dll, userData, 0L, WebView2Navigation.builtinHtml(), new WebView2Host.Listener() {
            @Override
            public void message(String json) {
                System.out.println("message " + json);
                if (json.contains("ready")) done.countDown();
            }

            @Override
            public void failed(String detail, boolean missingRuntime) {
                failure.set(detail);
                done.countDown();
            }
        });
        if (!done.await(20, TimeUnit.SECONDS)) failure.compareAndSet(null, "timed out waiting for the page");
        WebView2Host.close();
        Thread.sleep(400);
        WebView2Host.shutdown();
        if (failure.get() != null) throw new IllegalStateException(failure.get());
        System.out.println("webview2 window ok");
    }
}
