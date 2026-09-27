package net.exmo.exworld.mystery.blueprint;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.exmo.exworld.Exworld;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/** Datapack reload atomically replaces defaults; published world edits override them next game. */
public final class BlueprintLibrary extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, String> defaults = Map.of();

    public BlueprintLibrary() { super(new Gson(), "exworld_mystery/blueprints"); }
    public static void registerReload(AddReloadListenerEvent event) { event.addListener(new BlueprintLibrary()); }

    @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        try {
            Map<String, String> values = new LinkedHashMap<>();
            for (var entry : files.entrySet()) {
                String json = entry.getValue().toString();
                BlueprintGraph graph = BlueprintGraph.parse(json);
                values.put(graph.id().toString(), json);
            }
            defaults = Map.copyOf(values);
            Exworld.LOGGER.info("Loaded {} mystery blueprints", values.size());
        } catch (RuntimeException error) {
            Exworld.LOGGER.error("Mystery blueprint reload rejected; previous definitions remain active", error);
        }
    }

    public static void beginGame(MinecraftServer server) {
        Map<String, String> next = new LinkedHashMap<>(defaults);
        BlueprintStore store = BlueprintStore.get(server);
        next.putAll(store.published);
        for (String json : next.values()) BlueprintGraph.parse(json).validateResources(server);
        store.active.clear();
        store.active.putAll(next);
        store.pending.clear();
        store.fired.clear();
        store.changed();
    }

    public static String defaultJson(String id) { return defaults.get(id); }
}
