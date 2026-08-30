package net.exmo.exworld.battle.persistence;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public final class BattleSavedData extends SavedData {
    public static final Factory<BattleSavedData> FACTORY = new Factory<>(BattleSavedData::new, BattleSavedData::load);
    private final Map<UUID, CompoundTag> sessions = new LinkedHashMap<>();
    public Collection<CompoundTag> sessions() { return sessions.values().stream().map(CompoundTag::copy).toList(); }
    public void put(UUID id, CompoundTag tag) { sessions.put(id, tag.copy()); setDirty(); }
    public void remove(UUID id) { if (sessions.remove(id) != null) setDirty(); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag values = new ListTag(); sessions.values().forEach(value -> values.add(value.copy())); tag.put("sessions", values); return tag; }
    private static BattleSavedData load(CompoundTag tag, HolderLookup.Provider registries) { BattleSavedData data = new BattleSavedData(); ListTag values = tag.getList("sessions", Tag.TAG_COMPOUND); for (int i = 0; i < values.size(); i++) { CompoundTag value = values.getCompound(i); data.sessions.put(value.getUUID("id"), value.copy()); } return data; }
}
