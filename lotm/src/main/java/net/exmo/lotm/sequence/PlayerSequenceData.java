package net.exmo.lotm.sequence;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;

/** Persistent sequence identity and passive cooldown table. */
public final class PlayerSequenceData implements INBTSerializable<CompoundTag> {
    private String pathway = "";
    private String rank = "";
    private final Map<String, Long> passiveReadyAt = new HashMap<>();

    public boolean hasSequence() {
        return !pathway.isBlank() && !rank.isBlank();
    }

    public String pathway() {
        return pathway;
    }

    public String rank() {
        return rank;
    }

    public void set(ResourceLocation pathwayId, SequenceRank sequenceRank) {
        this.pathway = pathwayId == null ? "" : pathwayId.toString();
        this.rank = sequenceRank == null ? "" : sequenceRank.token();
    }

    public void clear() {
        pathway = "";
        rank = "";
        passiveReadyAt.clear();
    }

    public long readyAt(ResourceLocation passiveId) {
        if (passiveId == null) return 0L;
        return passiveReadyAt.getOrDefault(passiveId.toString(), 0L);
    }

    public void setReadyAt(ResourceLocation passiveId, long gameTime) {
        if (passiveId == null) return;
        passiveReadyAt.put(passiveId.toString(), gameTime);
    }

    public void copyFrom(PlayerSequenceData other) {
        if (other == null) return;
        pathway = other.pathway;
        rank = other.rank;
        passiveReadyAt.clear();
        passiveReadyAt.putAll(other.passiveReadyAt);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("pathway", pathway);
        tag.putString("rank", rank);
        CompoundTag cooldowns = new CompoundTag();
        passiveReadyAt.forEach(cooldowns::putLong);
        tag.put("cooldowns", cooldowns);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        pathway = tag.getString("pathway");
        rank = tag.getString("rank");
        passiveReadyAt.clear();
        CompoundTag cooldowns = tag.getCompound("cooldowns");
        for (String key : cooldowns.getAllKeys()) {
            passiveReadyAt.put(key, cooldowns.getLong(key));
        }
        if (tag.contains("legacy", Tag.TAG_LIST)) {
            ListTag ignored = tag.getList("legacy", Tag.TAG_STRING);
            for (int i = 0; i < ignored.size(); i++) {
                if (!(ignored.get(i) instanceof StringTag)) break;
            }
        }
    }
}
