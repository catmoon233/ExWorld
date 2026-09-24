package net.exmo.lotm.phone;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/** Optional ApricityUI phone. The document lives in lotm at assets/apricityui/apricity/lotm/phone.html. */
final class PhoneApricity {
    private static final String PATH = "lotm/phone.html";
    private static boolean listening;
    private static boolean pending;
    private static Object bound;
    private static int waits;
    private PhoneApricity() {}

    static void open() throws ReflectiveOperationException {
        listen();
        Class.forName("com.sighs.apricityui.ApricityUI").getMethod("screen", String.class).invoke(null, PATH);
        pending = true;
        waits = 40;
        Minecraft.getInstance().mouseHandler.releaseMouse();
    }

    static void close() {
        pending = false;
        bound = null;
        try {
            Class.forName("com.sighs.apricityui.ApricityUI").getMethod("closeScreen").invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    static void apply(String json) {
        Object document = document();
        if (document == null) return;
        JsonObject body = parse(json);
        text(document, "status", text(body, "status").isBlank() ? "已连接" : text(body, "status"));
        notice(document, text(body, "status"));
        if (body.has("camera") && !body.get("camera").isJsonNull()) {
            text(document, "camera-flag", body.get("camera").getAsBoolean() ? "1" : "0");
        }
        text(document, "gold", String.valueOf(number(body, "gold")));
        text(document, "call-line", text(body, "call").isBlank() ? "未通话" : "通话中：" + text(body, "call"));
        text(document, "mods", (text(body, "number").isBlank() ? "未安装 SIM" : text(body, "number")) + " · 微信 " + text(body, "wechat"));
        boolean online = body.has("online") && !body.get("online").isJsonNull() && body.get("online").getAsBoolean();
        text(document, "net-line", online ? (text(body, "wifi").isBlank() ? "微信可用 · 流量 " + number(body, "data") : "微信可用 · WiFi " + text(body, "wifi")) : "未连接 WiFi，且没有可用流量，微信不可用");
         text(document, "bill-line", "话费 " + number(body, "credit") + " · 流量 " + number(body, "data"));
         text(document, "battery", number(body, "battery") + "%");
        theme(document, text(body, "theme").isBlank() ? "light" : text(body, "theme"));
        StringBuilder nets = new StringBuilder();
        if (body.has("wifis") && body.get("wifis").isJsonArray()) {
            body.getAsJsonArray("wifis").forEach(element -> {
                JsonObject item = element.getAsJsonObject();
                nets.append(text(item, "name")).append(' ').append(number(item, "x")).append(',').append(number(item, "y")).append(',').append(number(item, "z")).append('\n');
            });
        }
        text(document, "wifi-list", nets.isEmpty() ? "范围内没有 WiFi" : nets.toString().strip());
        StringBuilder chats = new StringBuilder();
        if (body.has("groups") && body.get("groups").isJsonArray()) {
            body.getAsJsonArray("groups").forEach(element -> {
                JsonObject group = element.getAsJsonObject();
                chats.append("群 ").append(text(group, "name")).append(" → g:").append(text(group, "id")).append('\n');
            });
        }
        if (body.has("album") && body.get("album").isJsonArray()) {
            body.getAsJsonArray("album").forEach(element -> {
                JsonObject photo = element.getAsJsonObject();
                chats.append("照片 #").append(photo.has("slot") ? photo.get("slot").getAsInt() : 0).append(' ').append(text(photo, "name")).append('\n');
            });
        }
        if (body.has("messages") && body.get("messages").isJsonArray()) {
            body.getAsJsonArray("messages").forEach(element -> {
                JsonObject message = element.getAsJsonObject();
                chats.append(text(message, "from")).append(" → ").append(text(message, "to")).append("：").append(text(message, "text"));
                String extra = text(message, "extra");
                if (!extra.isBlank()) chats.append(" [").append(extra).append(']');
                chats.append('\n');
            });
        }
        text(document, "chats", chats.isEmpty() ? "还没有消息" : chats.toString().strip());
        StringBuilder players = new StringBuilder();
        if (body.has("players") && body.get("players").isJsonArray()) {
            body.getAsJsonArray("players").forEach(element -> {
                if (!players.isEmpty()) players.append("、");
                players.append(element.getAsString());
            });
        }
        text(document, "contacts", players.isEmpty() ? "没有其他在线玩家" : players.toString());
    }

    private static void listen() {
        if (listening) return;
        listening = true;
        NeoForge.EVENT_BUS.addListener(PhoneApricity::tick);
    }
     private static int clocks;
     private static void tick(ClientTickEvent.Post event) {
         Object open = document();
         if (open != null && ++clocks >= 20) {
             clocks = 0;
             clock(open);
         }
         if (!pending) return;
         Object document = document();
         if (document != null && document != bound) {
             bind(document);
             bound = document;
             pending = false;
             return;
         }
         if (--waits <= 0) pending = false;
     }
 
     private static void clock(Object document) {
         java.time.LocalDateTime now = java.time.LocalDateTime.now();
         String time = String.format("%02d:%02d", now.getHour(), now.getMinute());
         String date = now.getMonthValue() + "月" + now.getDayOfMonth() + "日 " + "周" + "日一二三四五六".charAt(now.getDayOfWeek().getValue() % 7);
        text(document, "clock", time);
        text(document, "lock-time", time);
        text(document, "lock-date", date);
        text(document, "home-time", time);
        text(document, "home-weather", weather());
        text(document, "lock-weather", weather());
    }

    private static String weather() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return "晴 26°";
        if (minecraft.level.isThundering()) return "雷暴 16°";
        if (minecraft.level.isRaining()) return "雨 18°";
        return "晴 26°";
    }
    private static void theme(Object document, String value) {
        String theme = "dark".equals(value) ? "dark" : "light";
        text(document, "theme-name", theme);
        Object screen = element(document, "screen");
        if (screen == null) return;
        try {
            screen.getClass().getMethod("setAttribute", String.class, String.class).invoke(screen, "data-theme", theme);
        } catch (ReflectiveOperationException ignored) {
        }
    }


    private static void bind(Object document) {
        listen(document, "send", () -> send("{\"type\":\"phone\",\"action\":\"chat\",\"to\":" + quote(value(document, "chat-to")) + ",\"text\":" + quote(value(document, "chat-text")) + "}"));
        listen(document, "friend", () -> send("{\"type\":\"phone\",\"action\":\"friend\",\"to\":" + quote(value(document, "friend-name")) + "}"));
        listen(document, "rename-go", () -> send("{\"type\":\"phone\",\"action\":\"rename\",\"text\":" + quote(value(document, "rename")) + "}"));
        listen(document, "moment", () -> send("{\"type\":\"phone\",\"action\":\"moment\",\"text\":" + quote(value(document, "moment-text")) + "}"));
        listen(document, "sms-go", () -> send("{\"type\":\"phone\",\"action\":\"sms\",\"to\":" + quote(value(document, "sms-to")) + ",\"text\":" + quote(value(document, "sms-text")) + "}"));
        listen(document, "pay-go", () -> send("{\"type\":\"phone\",\"action\":\"pay\",\"to\":" + quote(value(document, "pay-to")) + ",\"amount\":" + amount(value(document, "pay-amount")) + "}"));
        listen(document, "npc-save", () -> send("{\"type\":\"phone\",\"action\":\"npc\",\"id\":" + quote(value(document, "npc-id")) + ",\"number\":" + quote(value(document, "npc-number")) + ",\"wechat\":" + quote(value(document, "npc-wechat")) + ",\"text\":" + quote(value(document, "npc-moment")) + "}"));
        listen(document, "shoot", () -> send("{\"type\":\"phone\",\"action\":\"camera\"}"));
        listen(document, "call", () -> send("{\"type\":\"phone\",\"action\":\"call\",\"to\":" + quote(value(document, "call-to")) + "}"));
        listen(document, "hangup", () -> send("{\"type\":\"phone\",\"action\":\"hangup\"}"));
        listen(document, "group-go", () -> send("{\"type\":\"phone\",\"action\":\"group\",\"text\":" + quote(value(document, "group-name")) + ",\"to\":" + quote(value(document, "group-members")) + "}"));
        listen(document, "photo-go", () -> send("{\"type\":\"phone\",\"action\":\"photo\",\"to\":" + quote(value(document, "chat-to")) + ",\"slot\":" + amount(value(document, "photo-slot")) + "}"));
        listen(document, "transfer-go", () -> send("{\"type\":\"phone\",\"action\":\"transfer\",\"to\":" + quote(value(document, "chat-to")) + ",\"amount\":" + amount(value(document, "money")) + "}"));
        listen(document, "redpack-go", () -> send("{\"type\":\"phone\",\"action\":\"redpack\",\"to\":" + quote(value(document, "chat-to")) + ",\"amount\":" + amount(value(document, "money")) + ",\"shares\":" + amount(value(document, "shares")) + "}"));
        listen(document, "voice-go", () -> send("{\"type\":\"phone\",\"action\":\"voice\",\"to\":" + quote(value(document, "chat-to")) + "}"));
        listen(document, "claim-go", () -> send("{\"type\":\"phone\",\"action\":\"claim\",\"id\":" + quote(value(document, "claim-id")) + "}"));
        listen(document, "topup", () -> send("{\"type\":\"phone\",\"action\":\"topup\",\"amount\":" + amount(value(document, "bill-amount")) + "}"));
        listen(document, "buy-data", () -> send("{\"type\":\"phone\",\"action\":\"data\",\"amount\":" + amount(value(document, "bill-amount")) + "}"));
        listen(document, "wifi-go", () -> send("{\"type\":\"phone\",\"action\":\"wifi\",\"name\":" + quote(value(document, "wifi-name")) + ",\"text\":" + quote(value(document, "wifi-pass")) + "}"));
        listen(document, "wifi-off", () -> send("{\"type\":\"phone\",\"action\":\"unwifi\"}"));
        listen(document, "theme-light", () -> send("{\"type\":\"phone\",\"action\":\"setting\",\"name\":\"theme\",\"text\":\"light\"}"));
        listen(document, "theme-dark", () -> send("{\"type\":\"phone\",\"action\":\"setting\",\"name\":\"theme\",\"text\":\"dark\"}"));
    }

    private static void listen(Object document, String id, Runnable action) {
        Object element = element(document, id);
        if (element == null) return;
        try {
            Method method = element.getClass().getMethod("addEventListener", String.class, Consumer.class);
            method.invoke(element, "click", (Consumer<Object>) ignored -> action.run());
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void send(String json) {
        PhoneClient.fromPage(json);
    }

    private static Object document() {
        try {
            Object documents = Class.forName("com.sighs.apricityui.init.Document").getMethod("get", String.class).invoke(null, PATH);
            if (!(documents instanceof java.util.List<?> list) || list.isEmpty()) return null;
            return list.get(list.size() - 1);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object element(Object document, String id) {
        try {
            return document.getClass().getMethod("getElementById", String.class).invoke(document, id);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
    private static void notice(Object document, String status) {
        boolean show = status != null && !status.isBlank() && !"已连接".equals(status) && !"正在连接…".equals(status);
        text(document, "notice", show ? status : "");
        Object element = element(document, "notice");
        if (element == null) return;
        try {
            element.getClass().getMethod("setAttribute", String.class, String.class)
                    .invoke(element, "class", show ? "wx-notice on" : "wx-notice");
        } catch (ReflectiveOperationException ignored) {
        }
    }


    private static void text(Object document, String id, String value) {
        Object element = element(document, id);
        if (element == null) return;
        try {
            element.getClass().getMethod("setInnerText", String.class).invoke(element, value == null ? "" : value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static String value(Object document, String id) {
        Object element = element(document, id);
        if (element == null) return "";
        try {
            Object field = element.getClass().getField("value").get(element);
            if (field != null && !field.toString().isBlank()) return field.toString().trim();
            Object attribute = element.getClass().getMethod("getAttribute", String.class).invoke(element, "value");
            return attribute == null ? "" : attribute.toString().trim();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static int amount(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static JsonObject parse(String json) {
        try {
            return JsonParser.parseString(json == null ? "{}" : json).getAsJsonObject();
        } catch (RuntimeException ignored) {
            return new JsonObject();
        }
    }

    private static String text(JsonObject body, String key) {
        return body.has(key) && !body.get(key).isJsonNull() ? body.get(key).getAsString() : "";
    }

    private static int number(JsonObject body, String key) {
        try {
            return body.has(key) ? body.get(key).getAsInt() : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static boolean flag(JsonObject body, String key) {
        return body.has(key) && body.get(key).getAsBoolean();
    }
}
