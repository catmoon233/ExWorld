package net.exmo.exworld.monster;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** World-local enabled-package order. Entries later in the list have higher precedence. */
public final class MonsterPackageSavedData extends SavedData {
    public static final Factory<MonsterPackageSavedData> FACTORY = new Factory<>(MonsterPackageSavedData::new, MonsterPackageSavedData::load);
    private final List<String> enabled = new ArrayList<>();
    public List<String> enabled() { return List.copyOf(enabled); }
    public void setEnabled(List<String> ids) { enabled.clear(); ids.stream().filter(id -> id != null && !id.isBlank()).distinct().forEach(enabled::add); setDirty(); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag(); enabled.forEach(id -> list.add(StringTag.valueOf(id))); tag.put("enabled", list); return tag;
    }
    private static MonsterPackageSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        MonsterPackageSavedData data = new MonsterPackageSavedData(); ListTag list = tag.getList("enabled", CompoundTag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) data.enabled.add(list.getString(i)); return data;
    }
}
