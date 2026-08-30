package net.exmo.exworld.equipment;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** World-persistent player equipment-slot configuration. */
public final class PlayerEquipmentSavedData extends SavedData {
    public static final Factory<PlayerEquipmentSavedData> FACTORY =
            new Factory<>(PlayerEquipmentSavedData::new, PlayerEquipmentSavedData::load);
    private final Map<UUID, Slots> players = new LinkedHashMap<>();

    public Slots get(UUID playerId) {
        return players.computeIfAbsent(playerId, ignored -> new Slots());
    }

    public void set(UUID playerId, int slot, String itemId) {
        Slots slots = get(playerId);
        if (slot == 1) slots.first = itemId == null ? "" : itemId;
        else if (slot == 2) slots.second = itemId == null ? "" : itemId;
        else throw new IllegalArgumentException("equipment slot must be 1 or 2");
        setDirty();
    }

    public static final class Slots {
        private String first = "";
        private String second = "";
        public String first() { return first; }
        public String second() { return second; }
        public String get(int slot) { return slot == 1 ? first : slot == 2 ? second : ""; }
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("version", 1);
        ListTag list = new ListTag();
        players.forEach((id, slots) -> {
            CompoundTag value = new CompoundTag();
            value.putUUID("player", id);
            value.putString("slot_1", slots.first);
            value.putString("slot_2", slots.second);
            list.add(value);
        });
        tag.put("players", list);
        return tag;
    }

    private static PlayerEquipmentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerEquipmentSavedData data = new PlayerEquipmentSavedData();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag value = list.getCompound(i);
            Slots slots = new Slots();
            slots.first = value.getString("slot_1");
            slots.second = value.getString("slot_2");
            data.players.put(value.getUUID("player"), slots);
        }
        return data;
    }
}
