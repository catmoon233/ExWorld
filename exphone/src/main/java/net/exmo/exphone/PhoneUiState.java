package net.exmo.exphone;

import java.util.*;

/** Pure navigation/draft state, independent of AUI and Minecraft. */
final class PhoneUiState {
    String route = "lock";
    String overlay = "";
    int desktop;
    final Deque<String> history = new ArrayDeque<>();
    final LinkedHashSet<String> recents = new LinkedHashSet<>();
    final Map<String, String> drafts = new HashMap<>();
    final Map<String, Double> scroll = new HashMap<>();
    final Map<String, String> read = new HashMap<>();

    void go(String next) {
        if (next.equals(route)) return;
        if (next.equals("home") || next.equals("lock")) history.clear();
        else history.addLast(route);
        route = next; overlay = "";
        if (!next.equals("home") && !next.equals("lock")) {
            String app = next.startsWith("chat:") || Set.of("contacts", "moments", "me", "group", "friend").contains(next) ? "wechat" : next.split(":", 2)[0];
            recents.remove(app); recents.add(app);
            while (recents.size() > 8) recents.remove(recents.iterator().next());
        }
    }
    boolean back() {
        if (!overlay.isEmpty()) { overlay = ""; return true; }
        if (route.equals("home") || route.equals("lock")) return false;
        route = history.isEmpty() ? "home" : history.removeLast();
        return true;
    }
    String draft(String id) { return drafts.getOrDefault(route + "/" + id, ""); }
    void draft(String id, String value) { drafts.put(route + "/" + id, value); }
}
