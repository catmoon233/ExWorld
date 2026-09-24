package net.exmo.exworld.client.webview;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Thread-safe open/close/post queue. An unsupported platform drops every request. */
public final class WebView2Requests {
    public enum Kind { OPEN, CLOSE, POST }

    public record Request(Kind kind, String payload) {}

    private final boolean platformSupported;
    private final Queue<Request> pending = new ConcurrentLinkedQueue<>();
    private int ignored;

    public WebView2Requests(boolean platformSupported) {
        this.platformSupported = platformSupported;
    }

    public boolean platformSupported() {
        return platformSupported;
    }

    public void open() {
        submit(Kind.OPEN, "");
    }

    public void close() {
        submit(Kind.CLOSE, "");
    }

    public void post(String json) {
        submit(Kind.POST, json == null ? "" : json);
    }

    public synchronized int ignored() {
        return ignored;
    }

    public List<Request> drain() {
        List<Request> out = new ArrayList<>();
        Request next;
        while ((next = pending.poll()) != null) out.add(next);
        return out;
    }

    private synchronized void submit(Kind kind, String payload) {
        if (!platformSupported) {
            ignored++;
            return;
        }
        pending.add(new Request(kind, payload));
    }
}
