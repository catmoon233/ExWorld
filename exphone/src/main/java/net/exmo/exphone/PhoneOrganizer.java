package net.exmo.exphone;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Local alarms and calendar events for one world and one player. */
final class PhoneOrganizer {
    private static Path file;
    private static final List<JsonObject> alarms = new ArrayList<>();
    private static final List<JsonObject> events = new ArrayList<>();
    private static boolean loaded;

    static void reset() { file = null; loaded = false; alarms.clear(); events.clear(); }

    static void open() {
        if (loaded) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String world = mc.hasSingleplayerServer()
                ? mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().toString()
                : mc.getCurrentServer() == null ? "unknown" : mc.getCurrentServer().ip;
        String scope = UUID.nameUUIDFromBytes(world.getBytes(StandardCharsets.UTF_8)).toString();
        file = mc.gameDirectory.toPath().resolve("phone-organizer").resolve(scope)
                .resolve(mc.getUser().getProfileId() + ".json");
        loaded = true;
        if (!Files.isRegularFile(file)) return;
        try {
            JsonObject saved = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (saved.has("alarms")) saved.getAsJsonArray("alarms").forEach(item -> alarms.add(item.getAsJsonObject()));
            if (saved.has("events")) saved.getAsJsonArray("events").forEach(item -> events.add(item.getAsJsonObject()));
        } catch (Exception error) { PhoneApricity.notice("日历数据读取失败"); }
    }

    static List<JsonObject> alarms() { open(); return List.copyOf(alarms); }
    static List<JsonObject> events(long day) {
        open();return events.stream().filter(item -> item.get("day").getAsLong() == day).toList();
    }
    static String addAlarm(int hour, int minute, String label) {
        open();
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return "请输入有效时间";
        JsonObject alarm = new JsonObject();
        alarm.addProperty("id", UUID.randomUUID().toString()); alarm.addProperty("hour", hour); alarm.addProperty("minute", minute);
        alarm.addProperty("label", label.isBlank() ? "闹钟" : label.substring(0, Math.min(24, label.length())));
        alarm.addProperty("lastDay", -1);alarms.add(alarm);save();return "闹钟已设置";
    }
    static void removeAlarm(String id) { alarms.removeIf(item -> item.get("id").getAsString().equals(id));save(); }
    static String addEvent(long day, String title) {
        open();if (title.isBlank()) return "请填写日程内容";
        JsonObject event = new JsonObject();event.addProperty("id", UUID.randomUUID().toString());
        event.addProperty("day", day);event.addProperty("title", title.substring(0, Math.min(80, title.length())));
        events.add(event);save();return "日程已保存";
    }
    static void removeEvent(String id) { events.removeIf(item -> item.get("id").getAsString().equals(id));save(); }

    static void tick(long dayTime) {
        open();if (!loaded) return;
        long day = Math.floorDiv(dayTime, 24_000L);
        String clock = PhoneClock.time(dayTime);
        for (JsonObject alarm : alarms) {
            String when = String.format(java.util.Locale.ROOT, "%02d:%02d", alarm.get("hour").getAsInt(), alarm.get("minute").getAsInt());
            if (!when.equals(clock) || alarm.get("lastDay").getAsLong() >= day) continue;
            alarm.addProperty("lastDay", day);save();
            String message = "闹钟 · " + alarm.get("label").getAsString();
            PhoneApricity.notice(message);PhoneApricity.sound();
            if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.displayClientMessage(Component.literal(message), true);
        }
    }

    private static void save() {
        if (file == null) return;
        JsonObject saved = new JsonObject();JsonArray alarmArray = new JsonArray(), eventArray = new JsonArray();
        alarms.forEach(alarmArray::add);events.forEach(eventArray::add);
        saved.add("alarms", alarmArray);saved.add("events", eventArray);
        try { Files.createDirectories(file.getParent());Files.writeString(file, saved.toString()); }
        catch (Exception error) { PhoneApricity.notice("日历数据保存失败：" + error.getMessage()); }
    }
}
