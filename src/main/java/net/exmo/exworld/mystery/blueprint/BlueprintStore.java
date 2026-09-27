package net.exmo.exworld.mystery.blueprint;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Draft, published and active versions stay distinct across restarts. */
public final class BlueprintStore extends SavedData {
    public static final Factory<BlueprintStore> FACTORY = new Factory<>(BlueprintStore::new, BlueprintStore::load);
    public final Map<String, String> drafts = new LinkedHashMap<>();
    public final Map<String, String> published = new LinkedHashMap<>();
    public final Map<String, String> active = new LinkedHashMap<>();
    public final List<Pending> pending = new ArrayList<>();
    public final Map<String, Boolean> fired = new LinkedHashMap<>();

    public record Pending(UUID run, String graph, String node, UUID player, String detail, long due) {}

    public static BlueprintStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "exworld_mystery_blueprints");
    }

    public void changed() { setDirty(); }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("drafts", writeMap(drafts));
        tag.put("published", writeMap(published));
        tag.put("active", writeMap(active));
        ListTag waits = new ListTag();
        for (Pending item : pending) {
            CompoundTag value = new CompoundTag();
            value.putUUID("run", item.run); value.putString("graph", item.graph);
            value.putString("node", item.node);
            if (item.player != null) value.putUUID("player", item.player);
            value.putString("detail", item.detail); value.putLong("due", item.due);
            waits.add(value);
        }
        tag.put("pending", waits);
        CompoundTag flags = new CompoundTag(); fired.keySet().forEach(key -> flags.putBoolean(key, true)); tag.put("fired", flags);
        return tag;
    }

    private static BlueprintStore load(CompoundTag tag, HolderLookup.Provider registries) {
        BlueprintStore store = new BlueprintStore();
        readMap(tag.getCompound("drafts"), store.drafts);
        readMap(tag.getCompound("published"), store.published);
        readMap(tag.getCompound("active"), store.active);
        ListTag waits = tag.getList("pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < waits.size(); i++) {
            CompoundTag value = waits.getCompound(i);
            if (!value.hasUUID("run")) continue;
            store.pending.add(new Pending(value.getUUID("run"), value.getString("graph"), value.getString("node"),
                    value.hasUUID("player") ? value.getUUID("player") : null, value.getString("detail"), value.getLong("due")));
        }
        CompoundTag flags = tag.getCompound("fired");
        flags.getAllKeys().forEach(key -> store.fired.put(key, true));
        return store;
    }

    private static CompoundTag writeMap(Map<String, String> map) {
        CompoundTag tag = new CompoundTag(); map.forEach(tag::putString); return tag;
    }

    private static void readMap(CompoundTag tag, Map<String, String> into) {
        tag.getAllKeys().forEach(key -> into.put(key, tag.getString(key)));
    }
}
