package net.exmo.exworld.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/** World-persistent item-id footprint table. */
public final class FootprintRulesSavedData extends SavedData {
    public static final String NAME = "exworld_footprint_rules";
    public static final Factory<FootprintRulesSavedData> FACTORY =
            new Factory<>(FootprintRulesSavedData::create, FootprintRulesSavedData::load);
    private static FootprintRules fallback = FootprintRules.defaults();
    private final FootprintRules rules;

    private FootprintRulesSavedData(FootprintRules rules) {
        this.rules = rules;
        fallback = rules;
    }

    private static FootprintRulesSavedData create() {
        return new FootprintRulesSavedData(FootprintRules.defaults());
    }

    public static FootprintRules fallback() {
        return fallback;
    }

    public static FootprintRulesSavedData get(MinecraftServer server) {
        FootprintRulesSavedData data = server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
        fallback = data.rules;
        return data;
    }

    public FootprintRules rules() {
        return rules;
    }

    public void replace(Map<String, ItemFootprint> values) {
        rules.replaceAll(values);
        fallback = rules;
        setDirty();
    }

    public void set(String itemId, ItemFootprint footprint) {
        rules.set(itemId, footprint);
        fallback = rules;
        setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        rules.snapshot().forEach((id, footprint) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            entry.putString("size", footprint.token());
            list.add(entry);
        });
        tag.put("rules", list);
        return tag;
    }

    private static FootprintRulesSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FootprintRules rules = FootprintRules.defaults();
        ListTag list = tag.getList("rules", Tag.TAG_COMPOUND);
        Map<String, ItemFootprint> values = new LinkedHashMap<>(rules.snapshot());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            values.put(entry.getString("id"), ItemFootprint.parse(entry.getString("size")));
        }
        rules.replaceAll(values);
        return new FootprintRulesSavedData(rules);
    }
}
