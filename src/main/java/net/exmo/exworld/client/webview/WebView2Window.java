package net.exmo.exworld.client.webview;

import net.exmo.exworld.Exworld;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Single client window. Non-Windows requests are dropped and reported once. */
public final class WebView2Window {
    private static final WebView2Requests REQUESTS = new WebView2Requests(WebView2Platform.supported());
    private static final Set<String> reported = ConcurrentHashMap.newKeySet();
    private static final WebView2Host.Listener LISTENER = new WebView2Host.Listener() {
        @Override
        public void opened() {
            open = true;
        }

        @Override
        public void closed() {
            open = false;
            capturing = false;
            Minecraft.getInstance().execute(WebView2Window::grabMouse);
        }

        @Override
        public void failed(String detail, boolean missingRuntime) {
            open = false;
            capturing = false;
            Exworld.LOGGER.warn("[webview] {}", detail);
            once(missingRuntime ? "exworld.webview.missing_runtime" : "exworld.webview.failed");
            Minecraft.getInstance().execute(WebView2Window::grabMouse);
        }

        @Override
        public void message(String json) {
            if (WebView2Navigation.requestsClose(json)) {
                close();
                return;
            }
            String target = WebView2Navigation.navigateTarget(json);
            if (json != null && json.contains("\"navigate\"")) {
                if (!WebView2Navigation.allows(target)) Exworld.LOGGER.warn("Rejected WebView2 navigation to {}", target);
                return;
            }
            messages.accept(json == null ? "" : json);
        }
    };

    private static volatile boolean open;
    private static volatile boolean capturing;
    private static java.util.function.Consumer<String> messages = ignored -> {};

    private WebView2Window() {}

    public static boolean isOpen() {
        return open || capturing;
    }

    public static void open() {
        openHtml(null);
    }

    /** Opens the window on a page supplied by another mod. Returns false when WebView2 cannot run. */
    public static boolean openHtml(String html) {
        REQUESTS.open();
        if (!REQUESTS.platformSupported()) {
            REQUESTS.drain();
            once("exworld.webview.unsupported");
            return false;
        }
        REQUESTS.drain();
        capturing = true;
        releaseMouse();
        try {
            WebView2Host.setLog(message -> Exworld.LOGGER.info("[webview] {}", message));
            String page = html == null || html.isBlank() ? WebView2Navigation.builtinHtml() : html;
            WebView2Host.open(loader(), userData(), ownerHwnd(), page, LISTENER);
            return true;
        } catch (Exception exception) {
            capturing = false;
            Exworld.LOGGER.error("WebView2 open failed", exception);
            once("exworld.webview.failed");
            grabMouse();
            return false;
        }
    }

    public static void setMessageListener(java.util.function.Consumer<String> listener) {
        messages = listener == null ? ignored -> {} : listener;
    }

    public static void close() {
        REQUESTS.close();
        if (!REQUESTS.platformSupported()) return;
        REQUESTS.drain();
        WebView2Host.close();
    }

    public static void post(String json) {
        REQUESTS.post(json);
        if (!REQUESTS.platformSupported()) return;
        REQUESTS.drain();
        WebView2Host.post(json);
    }

    private static Path userData() {
        return FMLPaths.GAMEDIR.get().resolve("exworld").resolve("webview2");
    }

    private static long ownerHwnd() {
        return Win32.hwnd();
    }

    private static Path loader() throws IOException {
        Path dest = FMLPaths.GAMEDIR.get().resolve("exworld").resolve("natives").resolve("WebView2Loader.dll");
        try (InputStream in = WebView2Window.class.getResourceAsStream("/natives/windows-x64/WebView2Loader.dll")) {
            if (in == null) throw new IOException("missing natives/windows-x64/WebView2Loader.dll");
            byte[] bytes = in.readAllBytes();
            if (Files.exists(dest) && Files.size(dest) == bytes.length) return dest;
            Files.createDirectories(dest.getParent());
            Path tmp = dest.resolveSibling(dest.getFileName() + ".tmp");
            Files.write(tmp, bytes);
            try {
                Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            return dest;
        }
    }

    private static void once(String key) {
        if (!reported.add(key)) return;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable(key), false);
            else Exworld.LOGGER.info(Component.translatable(key).getString());
        });
    }

    private static void releaseMouse() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) minecraft.mouseHandler.releaseMouse();
    }

    private static void grabMouse() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) minecraft.mouseHandler.grabMouse();
    }

    /** Loaded only after the Windows check, so other platforms never touch the Win32 GLFW binding. */
    private static final class Win32 {
        private Win32() {}

        static long hwnd() {
            Minecraft minecraft = Minecraft.getInstance();
            return org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(minecraft.getWindow().getWindow());
        }
    }
}
