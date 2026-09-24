package net.exmo.exworld.client.webview;

import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The WebView2 and Win32 entry points this window actually calls. Not a full SDK binding. */
final class WebView2Com {
    static final int S_OK = 0;
    static final int E_NOINTERFACE = 0x80004002;
    static final int E_POINTER = 0x80004003;
    static final int ERROR_FILE_NOT_FOUND = 0x80070002;
    static final int ERROR_MOD_NOT_FOUND = 0x8007007E;

    static final byte[] IID_IUNKNOWN = iid("00000000-0000-0000-C000-000000000046");
    static final byte[] IID_ENV_HANDLER = iid("4e8a3389-c9d8-4bd2-b6b5-124fee6cc14d");
    static final byte[] IID_CONTROLLER_HANDLER = iid("6c4819f3-c9b7-4260-8127-c9f5bde7f68c");
    static final byte[] IID_SCRIPT_HANDLER = iid("b99369f3-9b11-47b5-bc6f-8e7895fcea17");
    static final byte[] IID_MESSAGE_HANDLER = iid("57213f19-00e6-49fa-8e07-898ea01ecbd2");

    private static Loader loader;
    private static String loaderPath;

    private WebView2Com() {}

    static byte[] iid(String guid) {
        String hex = guid.replace("-", "");
        byte[] raw = new byte[16];
        for (int i = 0; i < 16; i++) raw[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        byte[] out = new byte[16];
        out[0] = raw[3];
        out[1] = raw[2];
        out[2] = raw[1];
        out[3] = raw[0];
        out[4] = raw[5];
        out[5] = raw[4];
        out[6] = raw[7];
        out[7] = raw[6];
        System.arraycopy(raw, 8, out, 8, 8);
        return out;
    }

    static boolean failed(int hr) {
        return hr < 0;
    }

    static boolean missingRuntime(int hr) {
        return hr == ERROR_FILE_NOT_FOUND || hr == ERROR_MOD_NOT_FOUND;
    }

    static MemoryWide wide(String value) {
        MemoryWide memory = new MemoryWide((value.length() + 1L) * 2L);
        memory.setWideString(0, value);
        return memory;
    }

    static int call(Pointer object, int slot, Object... args) {
        Pointer vtable = object.getPointer(0);
        Pointer function = vtable.getPointer((long) slot * Native.POINTER_SIZE);
        Object[] full = new Object[args.length + 1];
        full[0] = object;
        System.arraycopy(args, 0, full, 1, args.length);
        return com.sun.jna.Function.getFunction(function, com.sun.jna.Function.ALT_CONVENTION).invokeInt(full);
    }

    static String takeWide(Pointer object, int slot) {
        com.sun.jna.Memory out = new com.sun.jna.Memory(Native.POINTER_SIZE);
        if (failed(call(object, slot, out))) return null;
        Pointer value = out.getPointer(0);
        if (value == null) return null;
        try {
            return value.getWideString(0);
        } finally {
            ole32().CoTaskMemFree(value);
        }
    }

    static User32 user32() {
        return User32.INSTANCE;
    }

    static Kernel32 kernel32() {
        return Kernel32.INSTANCE;
    }

    static Ole32 ole32() {
        return Ole32.INSTANCE;
    }

    static Loader loader(String path) {
        if (loader != null && path.equals(loaderPath)) return loader;
        loader = Native.load(path, Loader.class);
        loaderPath = path;
        return loader;
    }

    interface ResultHandler extends StdCallLibrary.StdCallCallback {
        int invoke(Pointer self, int hr, Pointer result);
    }

    interface EventHandler extends StdCallLibrary.StdCallCallback {
        int invoke(Pointer self, Pointer sender, Pointer args);
    }

    /** A COM object whose vtable is QueryInterface, AddRef, Release, and one extra method. */
    static final class CallbackObject {
        private final com.sun.jna.Memory vtable;
        private final com.sun.jna.Memory instance;
        private final List<Callback> pins = new ArrayList<>();
        private final byte[] expected;
        private final QI qi = (self, riid, ppv) -> queryInterface(riid, ppv);
        private final Count add = self -> 2;
        private final Count release = self -> 1;

        private CallbackObject(byte[] expected, Callback extra) {
            this.expected = expected;
            pins.add(qi);
            pins.add(add);
            pins.add(release);
            pins.add(extra);
            vtable = new com.sun.jna.Memory((long) pins.size() * Native.POINTER_SIZE);
            for (int i = 0; i < pins.size(); i++) {
                vtable.setPointer((long) i * Native.POINTER_SIZE, com.sun.jna.CallbackReference.getFunctionPointer(pins.get(i)));
            }
            instance = new com.sun.jna.Memory(Native.POINTER_SIZE);
            instance.setPointer(0, vtable);
        }

        Pointer pointer() {
            return instance;
        }

        private int queryInterface(Pointer riid, Pointer ppv) {
            if (ppv == null) return E_POINTER;
            if (riid != null && (Arrays.equals(riid.getByteArray(0, 16), IID_IUNKNOWN) || Arrays.equals(riid.getByteArray(0, 16), expected))) {
                ppv.setPointer(0, instance);
                return S_OK;
            }
            ppv.setPointer(0, Pointer.NULL);
            return E_NOINTERFACE;
        }
    }


    interface QI extends StdCallLibrary.StdCallCallback {
        int invoke(Pointer self, Pointer riid, Pointer ppv);
    }

    interface Count extends StdCallLibrary.StdCallCallback {
        int invoke(Pointer self);
    }

    static CallbackObject resultHandler(byte[] iid, ResultHandler handler) {
        return new CallbackObject(iid, handler);
    }

    static CallbackObject eventHandler(byte[] iid, EventHandler handler) {
        return new CallbackObject(iid, handler);
    }

    /** Keeps a strong type so callers can retain the wide-string allocation. */
    static final class MemoryWide extends com.sun.jna.Memory {
        private MemoryWide(long size) {
            super(size);
        }
    }

    interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class);

        int RegisterClassExW(Pointer wndClass);

        Pointer CreateWindowExW(int exStyle, Pointer className, Pointer windowName, int style, int x, int y, int width, int height,
                Pointer parent, Pointer menu, Pointer instance, Pointer param);

        boolean DestroyWindow(Pointer hwnd);

        boolean ShowWindow(Pointer hwnd, int command);

        boolean UpdateWindow(Pointer hwnd);

        boolean SetForegroundWindow(Pointer hwnd);

        Pointer SetFocus(Pointer hwnd);

        int GetMessageW(Pointer msg, Pointer hwnd, int min, int max);

        boolean PeekMessageW(Pointer msg, Pointer hwnd, int min, int max, int remove);

        boolean TranslateMessage(Pointer msg);

        Pointer DispatchMessageW(Pointer msg);

        void PostQuitMessage(int code);

        boolean PostThreadMessageW(int threadId, int msg, Pointer wParam, Pointer lParam);

        Pointer DefWindowProcW(Pointer hwnd, int msg, Pointer wParam, Pointer lParam);

        boolean GetClientRect(Pointer hwnd, Pointer rect);

        boolean GetWindowRect(Pointer hwnd, Pointer rect);

        boolean IsWindow(Pointer hwnd);

        int GetSystemMetrics(int index);

        Pointer LoadCursorW(Pointer instance, Pointer cursor);
    }

    interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        Pointer GetModuleHandleW(Pointer module);

        int GetCurrentThreadId();

        int GetLastError();
    }

    interface Ole32 extends StdCallLibrary {
        Ole32 INSTANCE = Native.load("ole32", Ole32.class);

        int CoInitializeEx(Pointer reserved, int coinit);

        void CoUninitialize();

        void CoTaskMemFree(Pointer pointer);
    }

    interface Loader extends StdCallLibrary {
        int CreateCoreWebView2EnvironmentWithOptions(Pointer browserExecutableFolder, Pointer userDataFolder,
                Pointer environmentOptions, Pointer environmentCreatedHandler);

        int GetAvailableCoreWebView2BrowserVersionString(Pointer browserExecutableFolder, Pointer versionInfo);
    }
}
