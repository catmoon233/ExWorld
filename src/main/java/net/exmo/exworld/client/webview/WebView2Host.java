package net.exmo.exworld.client.webview;

import com.sun.jna.Pointer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * One owned top-level window. COM calls stay on a dedicated STA thread; other threads only enqueue work.
 * The window is owned by the Minecraft HWND, not embedded as a child of the game framebuffer.
 */
public final class WebView2Host {
    private static final int WM_DESTROY = 0x0002;
    private static final int WM_SIZE = 0x0005;
    private static final int WM_CLOSE = 0x0010;
    private static final int WM_APP = 0x8000;
    private static final int WS_OVERLAPPEDWINDOW = 0x00CF0000;
    private static final int WS_VISIBLE = 0x10000000;
    private static final int WS_CLIPCHILDREN = 0x02000000;
    private static final int WS_EX_APPWINDOW = 0x00040000;
    private static final int SW_SHOW = 5;
    private static final int SW_RESTORE = 9;
    private static final int ERROR_CLASS_ALREADY_EXISTS = 1410;
    private static final String BRIDGE = """
            window.exworld = window.exworld || {
              post: function (message) {
                var payload = message;
                if (typeof message === 'string') {
                  try { payload = JSON.parse(message); }
                  catch (e) { payload = { type: 'message', text: message }; }
                }
                if (window.chrome && window.chrome.webview) window.chrome.webview.postMessage(payload);
              }
            };
            """;

    private static final Object LOCK = new Object();
    private static final Queue<Runnable> inbox = new ConcurrentLinkedQueue<>();
    private static final Queue<String> posts = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger epoch = new AtomicInteger();
    private static final java.util.List<Object> pins = new java.util.ArrayList<>();

    private static volatile Consumer<String> log = message -> {};
    private static volatile boolean open;
    private static volatile int threadId;
    private static Thread thread;
    private static CountDownLatch ready;
    private static String startError;
    private static Pointer window;
    private static Pointer environment;
    private static Pointer controller;
    private static Pointer webview;
    private static Pointer className;
    private static com.sun.jna.Memory classStruct;
    private static int classAtom;
    private static WndProc wndProc;
    private static Listener listener = Listener.NONE;
    private static int token;
    private static String html = "";
    private static boolean closing;
    private static WebView2Com.MemoryWide retainedUserData;
    private static WebView2Com.MemoryWide retainedHtml;
    private static WebView2Com.MemoryWide retainedScript;
    private static WebView2Com.CallbackObject environmentHandler;
    private static WebView2Com.CallbackObject controllerHandler;
    private static WebView2Com.CallbackObject scriptHandler;
    private static WebView2Com.CallbackObject messageHandler;

    private WebView2Host() {}

    public interface Listener {
        Listener NONE = new Listener() {};

        default void message(String json) {}

        default void opened() {}

        default void closed() {}

        default void failed(String detail, boolean missingRuntime) {}
    }

    interface WndProc extends com.sun.jna.win32.StdCallLibrary.StdCallCallback {
        Pointer invoke(Pointer hwnd, int msg, Pointer wParam, Pointer lParam);
    }

    public static void setLog(Consumer<String> sink) {
        log = sink == null ? message -> {} : sink;
    }

    public static boolean isOpen() {
        return open;
    }

    public static String availableVersion(Path loaderDll) {
        WebView2Com.Loader loader = WebView2Com.loader(loaderDll.toAbsolutePath().toString());
        com.sun.jna.Memory out = new com.sun.jna.Memory(com.sun.jna.Native.POINTER_SIZE);
        int hr = loader.GetAvailableCoreWebView2BrowserVersionString(Pointer.NULL, out);
        if (WebView2Com.failed(hr)) return null;
        Pointer value = out.getPointer(0);
        if (value == null) return null;
        try {
            return value.getWideString(0);
        } finally {
            WebView2Com.ole32().CoTaskMemFree(value);
        }
    }

    public static void open(Path loaderDll, Path userData, long ownerHwnd, String page, Listener sink) {
        int attempt = epoch.incrementAndGet();
        start();
        inbox.add(() -> openOnSta(attempt, loaderDll, userData, ownerHwnd, page, sink == null ? Listener.NONE : sink));
        wake();
    }

    public static void close() {
        epoch.incrementAndGet();
        if (!started()) return;
        inbox.add(WebView2Host::closeOnSta);
        wake();
    }

    public static void post(String json) {
        if (json == null || !started()) return;
        posts.add(json);
        inbox.add(WebView2Host::flushPosts);
        wake();
    }

    public static void shutdown() {
        epoch.incrementAndGet();
        if (!started()) return;
        inbox.add(() -> {
            closeOnSta();
            WebView2Com.user32().PostQuitMessage(0);
        });
        wake();
    }

    private static boolean started() {
        synchronized (LOCK) {
            return thread != null && thread.isAlive();
        }
    }

    private static void start() {
        CountDownLatch latch;
        synchronized (LOCK) {
            if (thread != null && thread.isAlive()) return;
            ready = new CountDownLatch(1);
            startError = null;
            threadId = 0;
            latch = ready;
            thread = new Thread(WebView2Host::runSta, "ExWorld-WebView2");
            thread.setDaemon(true);
            thread.start();
        }
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("WebView2 STA thread did not start");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("WebView2 STA thread interrupted", exception);
        }
        if (startError != null) throw new IllegalStateException(startError);
    }

    private static void runSta() {
        int hr = WebView2Com.ole32().CoInitializeEx(Pointer.NULL, 2);
        if (WebView2Com.failed(hr)) {
            startError = "CoInitializeEx 0x" + Integer.toHexString(hr);
            ready.countDown();
            return;
        }
        try {
            WebView2Com.user32().PeekMessageW(new com.sun.jna.Memory(48), Pointer.NULL, 0, 0, 0);
            threadId = WebView2Com.kernel32().GetCurrentThreadId();
            ready.countDown();
            com.sun.jna.Memory msg = new com.sun.jna.Memory(48);
            while (true) {
                int got = WebView2Com.user32().GetMessageW(msg, Pointer.NULL, 0, 0);
                if (got == 0 || got == -1) break;
                if (msg.getInt(8) == WM_APP) {
                    drain();
                    continue;
                }
                WebView2Com.user32().TranslateMessage(msg);
                WebView2Com.user32().DispatchMessageW(msg);
                drain();
            }
        } finally {
            releaseEnvironment();
            WebView2Com.ole32().CoUninitialize();
            open = false;
            threadId = 0;
        }
    }

    private static void wake() {
        int id = threadId;
        if (id != 0) WebView2Com.user32().PostThreadMessageW(id, WM_APP, Pointer.NULL, Pointer.NULL);
    }

    private static void drain() {
        Runnable job;
        while ((job = inbox.poll()) != null) {
            try {
                job.run();
            } catch (RuntimeException exception) {
                log.accept("WebView2 task failed: " + exception);
                fail(false, exception.toString());
            }
        }
    }

    private static void openOnSta(int attempt, Path loaderDll, Path userData, long ownerHwnd, String page, Listener sink) {
        if (attempt != epoch.get()) return;
        token = attempt;
        listener = sink;
        html = page == null ? "" : page;
        if (window != null && WebView2Com.user32().IsWindow(window)) {
            if (webview != null && !html.isBlank()) {
                retainedHtml = WebView2Com.wide(html);
                WebView2Com.call(webview, 6, retainedHtml);
            }
            focus(window);
            resize(window);
            return;
        }
        String version = availableVersion(loaderDll);
        if (version == null) {
            fail(true, "WebView2 Runtime is not installed");
            return;
        }
        log.accept("WebView2 Runtime " + version);
        window = createWindow(ownerHwnd);
        if (window == null) {
            fail(false, "CreateWindowExW " + WebView2Com.kernel32().GetLastError());
            return;
        }
        if (environment != null) {
            createController();
            return;
        }
        try {
            Files.createDirectories(userData);
        } catch (java.io.IOException exception) {
            fail(false, exception.toString());
            closeOnSta();
            return;
        }
        retainedUserData = WebView2Com.wide(userData.toAbsolutePath().toString());
        environmentHandler = WebView2Com.resultHandler(WebView2Com.IID_ENV_HANDLER, (self, result, created) -> {
            try {
                onEnvironment(attempt, result, created);
            } catch (RuntimeException exception) {
                fail(false, exception.toString());
            }
            return WebView2Com.S_OK;
        });
        pins.add(environmentHandler);
        int hr = WebView2Com.loader(loaderDll.toAbsolutePath().toString()).CreateCoreWebView2EnvironmentWithOptions(
                Pointer.NULL, retainedUserData, Pointer.NULL, environmentHandler.pointer());
        if (WebView2Com.failed(hr)) {
            fail(WebView2Com.missingRuntime(hr), "CreateCoreWebView2EnvironmentWithOptions 0x" + Integer.toHexString(hr));
            closeOnSta();
        }
    }

    private static void onEnvironment(int attempt, int hr, Pointer created) {
        if (attempt != epoch.get()) return;
        if (WebView2Com.failed(hr) || created == null) {
            fail(WebView2Com.missingRuntime(hr), "environment 0x" + Integer.toHexString(hr));
            closeOnSta();
            return;
        }
        WebView2Com.call(created, 1);
        environment = created;
        createController();
    }

    private static void createController() {
        if (environment == null || window == null) return;
        controllerHandler = WebView2Com.resultHandler(WebView2Com.IID_CONTROLLER_HANDLER, (self, result, created) -> {
            try {
                onController(result, created);
            } catch (RuntimeException exception) {
                fail(false, exception.toString());
            }
            return WebView2Com.S_OK;
        });
        pins.add(controllerHandler);
        int hr = WebView2Com.call(environment, 3, window, controllerHandler.pointer());
        if (WebView2Com.failed(hr)) {
            fail(false, "CreateCoreWebView2Controller 0x" + Integer.toHexString(hr));
            closeOnSta();
        }
    }

    private static void onController(int hr, Pointer created) {
        if (token != epoch.get()) {
            if (created != null) WebView2Com.call(created, 2);
            return;
        }
        if (WebView2Com.failed(hr) || created == null) {
            fail(false, "controller 0x" + Integer.toHexString(hr));
            closeOnSta();
            return;
        }
        WebView2Com.call(created, 1);
        controller = created;
        com.sun.jna.Memory out = new com.sun.jna.Memory(com.sun.jna.Native.POINTER_SIZE);
        int got = WebView2Com.call(controller, 25, out);
        if (WebView2Com.failed(got)) {
            fail(false, "CoreWebView2 0x" + Integer.toHexString(got));
            closeOnSta();
            return;
        }
        webview = out.getPointer(0);
        resize(window);
        WebView2Com.call(controller, 4, 1);
        messageHandler = WebView2Com.eventHandler(WebView2Com.IID_MESSAGE_HANDLER, (self, sender, args) -> {
            try {
                String json = WebView2Com.takeWide(args, 4);
                if (json != null) listener.message(json);
            } catch (RuntimeException exception) {
                log.accept("WebView2 message failed: " + exception);
            }
            return WebView2Com.S_OK;
        });
        pins.add(messageHandler);
        com.sun.jna.Memory tokenMemory = new com.sun.jna.Memory(8);
        int added = WebView2Com.call(webview, 34, messageHandler.pointer(), tokenMemory);
        if (WebView2Com.failed(added)) log.accept("add_WebMessageReceived 0x" + Integer.toHexString(added));
        retainedScript = WebView2Com.wide(BRIDGE);
        scriptHandler = WebView2Com.resultHandler(WebView2Com.IID_SCRIPT_HANDLER, (self, result, id) -> WebView2Com.S_OK);
        pins.add(scriptHandler);
        WebView2Com.call(webview, 27, retainedScript, scriptHandler.pointer());
        retainedHtml = WebView2Com.wide(html);
        int navigated = WebView2Com.call(webview, 6, retainedHtml);
        if (WebView2Com.failed(navigated)) {
            fail(false, "NavigateToString 0x" + Integer.toHexString(navigated));
            closeOnSta();
            return;
        }
        flushPosts();
        open = true;
        focus(window);
        resize(window);
        listener.opened();
    }

    private static void flushPosts() {
        if (webview == null) return;
        String json;
        while ((json = posts.poll()) != null) {
            String target = WebView2Navigation.navigateTarget(json);
            if (json.contains("\"navigate\"") && !WebView2Navigation.allows(target)) {
                log.accept("Rejected WebView2 navigation to " + target);
                continue;
            }
            WebView2Com.MemoryWide wide = WebView2Com.wide(json);
            int hr = WebView2Com.call(webview, 33, wide);
            if (WebView2Com.failed(hr)) log.accept("PostWebMessageAsString 0x" + Integer.toHexString(hr));
        }
    }

    private static void closeOnSta() {
        if (closing) return;
        closing = true;
        boolean notify = false;
        try {
            posts.clear();
            releaseController();
            Pointer hwnd = window;
            window = null;
            open = false;
            notify = hwnd != null;
            if (hwnd != null && WebView2Com.user32().IsWindow(hwnd)) WebView2Com.user32().DestroyWindow(hwnd);
        } finally {
            closing = false;
        }
        if (notify) listener.closed();
    }

    private static void releaseController() {
        Pointer page = webview;
        Pointer view = controller;
        webview = null;
        controller = null;
        if (view != null) {
            try {
                WebView2Com.call(view, 24);
            } catch (RuntimeException ignored) {
                // Closing a failed controller should not take the game down.
            }
            WebView2Com.call(view, 2);
        }
        if (page != null) WebView2Com.call(page, 2);
    }

    private static void releaseEnvironment() {
        releaseController();
        Pointer current = environment;
        environment = null;
        if (current != null) WebView2Com.call(current, 2);
    }

    private static void fail(boolean missingRuntime, String detail) {
        log.accept(detail);
        listener.failed(detail, missingRuntime);
    }

    private static void focus(Pointer hwnd) {
        WebView2Com.user32().ShowWindow(hwnd, SW_RESTORE);
        WebView2Com.user32().ShowWindow(hwnd, SW_SHOW);
        WebView2Com.user32().SetForegroundWindow(hwnd);
        WebView2Com.user32().SetFocus(hwnd);
    }

    private static Pointer createWindow(long ownerHwnd) {
        ensureClass();
        WebView2Com.MemoryWide title = WebView2Com.wide("ExWorld");
        pins.add(title);
        int width = 960;
        int height = 640;
        int x = 80;
        int y = 80;
        Pointer owner = ownerHwnd == 0 ? Pointer.NULL : Pointer.createConstant(ownerHwnd);
        com.sun.jna.Memory rect = new com.sun.jna.Memory(16);
        if (ownerHwnd != 0 && WebView2Com.user32().GetWindowRect(owner, rect)) {
            int left = rect.getInt(0);
            int top = rect.getInt(4);
            int right = rect.getInt(8);
            int bottom = rect.getInt(12);
            x = left + Math.max(0, (right - left - width) / 2);
            y = top + Math.max(0, (bottom - top - height) / 2);
        } else {
            int screenW = WebView2Com.user32().GetSystemMetrics(0);
            int screenH = WebView2Com.user32().GetSystemMetrics(1);
            x = Math.max(0, (screenW - width) / 2);
            y = Math.max(0, (screenH - height) / 2);
        }
        Pointer hwnd = WebView2Com.user32().CreateWindowExW(WS_EX_APPWINDOW, className, title,
                WS_OVERLAPPEDWINDOW | WS_VISIBLE | WS_CLIPCHILDREN, x, y, width, height, owner, Pointer.NULL,
                WebView2Com.kernel32().GetModuleHandleW(Pointer.NULL), Pointer.NULL);
        if (hwnd != null) WebView2Com.user32().UpdateWindow(hwnd);
        return hwnd;
    }

    private static void ensureClass() {
        if (classAtom != 0) return;
        wndProc = WebView2Host::onMessage;
        pins.add(wndProc);
        className = WebView2Com.wide("ExWorldWebView2");
        pins.add(className);
        classStruct = new com.sun.jna.Memory(80);
        classStruct.clear();
        classStruct.setInt(0, 80);
        classStruct.setInt(4, 0x0003);
        classStruct.setPointer(8, com.sun.jna.CallbackReference.getFunctionPointer(wndProc));
        classStruct.setPointer(24, WebView2Com.kernel32().GetModuleHandleW(Pointer.NULL));
        classStruct.setPointer(40, WebView2Com.user32().LoadCursorW(Pointer.NULL, Pointer.createConstant(32512)));
        classStruct.setPointer(48, Pointer.createConstant(6));
        classStruct.setPointer(64, className);
        int atom = WebView2Com.user32().RegisterClassExW(classStruct);
        if (atom == 0 && WebView2Com.kernel32().GetLastError() != ERROR_CLASS_ALREADY_EXISTS) {
            throw new IllegalStateException("RegisterClassExW " + WebView2Com.kernel32().GetLastError());
        }
        classAtom = atom == 0 ? 1 : atom;
    }

    private static Pointer onMessage(Pointer hwnd, int msg, Pointer wParam, Pointer lParam) {
        try {
            if ((msg == WM_SIZE || msg == 0x0018) && hwnd.equals(window)) resize(hwnd);
            if (msg == WM_CLOSE) {
                closeOnSta();
                return Pointer.NULL;
            }
            if (msg == WM_DESTROY) return Pointer.NULL;
        } catch (RuntimeException exception) {
            log.accept("WebView2 window procedure failed: " + exception);
        }
        return WebView2Com.user32().DefWindowProcW(hwnd, msg, wParam, lParam);
    }

    private static void resize(Pointer hwnd) {
        if (controller == null || hwnd == null) return;
        com.sun.jna.Memory rect = new com.sun.jna.Memory(16);
        rect.clear();
        int width = 960;
        int height = 640;
        if (WebView2Com.user32().GetClientRect(hwnd, rect)) {
            int measuredW = rect.getInt(8) - rect.getInt(0);
            int measuredH = rect.getInt(12) - rect.getInt(4);
            if (measuredW >= 64 && measuredH >= 64) {
                width = measuredW;
                height = measuredH;
            }
        }
        com.sun.jna.Memory bounds = new com.sun.jna.Memory(16);
        bounds.clear();
        bounds.setInt(0, 0);
        bounds.setInt(4, 0);
        bounds.setInt(8, width);
        bounds.setInt(12, height);
        WebView2Com.call(controller, 6, bounds);
        WebView2Com.call(controller, 23);
    }
}
