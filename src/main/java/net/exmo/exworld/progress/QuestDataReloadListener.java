package net.exmo.exworld.progress;

import com.google.gson.*;
import java.util.*;
import net.exmo.exworld.Exworld;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/** Parses all quest files before replacing the live catalog. */
public final class QuestDataReloadListener extends SimpleJsonResourceReloadListener {
    public QuestDataReloadListener() { super(new Gson(), "exworld_quests"); }
    public static void register(AddReloadListenerEvent event) { event.addListener(new QuestDataReloadListener()); }
    @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        try {
            List<QuestDefinition> values = new ArrayList<>();
            for (var entry : files.entrySet()) values.add(parse(entry.getKey(), entry.getValue().getAsJsonObject()));
            QuestContent.replace(values); Exworld.LOGGER.info("Atomically loaded {} quest definitions", values.size());
        } catch (Exception error) { Exworld.LOGGER.error("Rejected complete quest content reload; previous snapshot remains active", error); }
    }
    private static QuestDefinition parse(ResourceLocation file, JsonObject json) {
        ResourceLocation id = ResourceLocation.parse(string(json, "id", file.toString()));
        QuestKind kind = QuestKind.valueOf(string(json, "kind", "side").toUpperCase(Locale.ROOT));
        List<QuestNode> nodes = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("nodes")) nodes.add(node(element.getAsJsonObject()));
        return new QuestDefinition(id, kind, string(json, "title_key", id + ".title"), string(json, "description_key", id + ".description"), json.get("entry").getAsString(), nodes);
    }
    private static QuestNode node(JsonObject json) {
        List<QuestObjective> objectives = new ArrayList<>(); if (json.has("objectives")) for (JsonElement value : json.getAsJsonArray("objectives")) objectives.add(objective(value.getAsJsonObject()));
        List<String> next = strings(json, "next"); List<QuestReward> rewards = new ArrayList<>(); if (json.has("rewards")) for (JsonElement value : json.getAsJsonArray("rewards")) rewards.add(reward(value.getAsJsonObject()));
        return new QuestNode(json.get("id").getAsString(), string(json, "title_key", ""), string(json, "description_key", ""), objectives, next, rewards);
    }
    private static QuestObjective objective(JsonObject json) {
        QuestObjective.Type type = QuestObjective.Type.valueOf(json.get("type").getAsString().toUpperCase(Locale.ROOT));
        ResourceLocation target = ResourceLocation.parse(json.get("target").getAsString());
        return new QuestObjective(type, target, string(json, "tag", ""), integer(json, "amount", 1), string(json, "dimension", ""), number(json, "x", 0), number(json, "y", 0), number(json, "z", 0), number(json, "radius", 4));
    }
    private static QuestReward reward(JsonObject json) {
        QuestReward.Kind kind = QuestReward.Kind.valueOf(json.get("type").getAsString().toUpperCase(Locale.ROOT));
        ResourceLocation id = kind == QuestReward.Kind.EXPERIENCE ? null : ResourceLocation.parse(string(json, "id", "exworld:gold"));
        return new QuestReward(kind, id, integer(json, "amount", 0), string(json, "value", ""));
    }
    private static String string(JsonObject value, String key, String fallback) { return value.has(key) ? value.get(key).getAsString() : fallback; }
    private static int integer(JsonObject value, String key, int fallback) { return value.has(key) ? value.get(key).getAsInt() : fallback; }
    private static double number(JsonObject value, String key, double fallback) { return value.has(key) ? value.get(key).getAsDouble() : fallback; }
    private static List<String> strings(JsonObject value, String key) { List<String> out = new ArrayList<>(); if (value.has(key)) value.getAsJsonArray(key).forEach(v -> out.add(v.getAsString())); return out; }
}
