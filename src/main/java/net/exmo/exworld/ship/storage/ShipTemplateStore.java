package net.exmo.exworld.ship.storage;

import net.exmo.exworld.ship.model.ShipTemplate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** World-scoped 飞船模板 store. Bytes stay gzip-compact; decoded templates are cached. */
public final class ShipTemplateStore extends SavedData {
    public static final String ID = "exworld_ship_templates";
    public static final Factory<ShipTemplateStore> FACTORY = new Factory<>(ShipTemplateStore::new, ShipTemplateStore::load);
    private static final int MAX_TEMPLATES = 256;

    private final Map<String, ShipTemplate> templates = new LinkedHashMap<>();

    public static ShipTemplateStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public List<ShipTemplate> all() { return new ArrayList<>(templates.values()); }

    public Optional<ShipTemplate> get(String id) { return Optional.ofNullable(templates.get(id)); }

    public void put(ShipTemplate template) {
        if (template.id().isBlank()) throw new IllegalArgumentException("template id is blank");
        if (!templates.containsKey(template.id()) && templates.size() >= MAX_TEMPLATES) {
            throw new IllegalArgumentException("too many ship templates");
        }
        templates.put(template.id(), template);
        setDirty();
    }

    public boolean remove(String id) {
        boolean removed = templates.remove(id) != null;
        if (removed) setDirty();
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ShipTemplate template : templates.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", template.id());
            entry.putByteArray("data", ShipNbtCodec.encodeTemplate(template));
            list.add(entry);
        }
        tag.put("templates", list);
        return tag;
    }

    private static ShipTemplateStore load(CompoundTag tag, HolderLookup.Provider registries) {
        ShipTemplateStore store = new ShipTemplateStore();
        ListTag list = tag.getList("templates", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            try {
                ShipTemplate template = ShipNbtCodec.decodeTemplate(entry.getByteArray("data"));
                store.templates.put(template.id(), template);
            } catch (RuntimeException ignored) {
            }
        }
        return store;
    }
}
