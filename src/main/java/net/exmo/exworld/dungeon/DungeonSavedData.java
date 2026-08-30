package net.exmo.exworld.dungeon;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public final class DungeonSavedData extends SavedData {
    public static final Factory<DungeonSavedData> FACTORY=new Factory<>(DungeonSavedData::new,DungeonSavedData::load);
    private final Map<UUID,DungeonRun> runs=new LinkedHashMap<>();
    public Collection<DungeonRun> runs(){return List.copyOf(runs.values());}
    public void put(DungeonRun run){runs.put(run.id(),run);setDirty();}
    public void remove(UUID id){if(runs.remove(id)!=null)setDirty();}
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries){ListTag values=new ListTag();runs.values().forEach(run->values.add(DungeonRunNbtCodec.save(run)));tag.put("runs",values);return tag;}
    private static DungeonSavedData load(CompoundTag tag, HolderLookup.Provider registries){DungeonSavedData data=new DungeonSavedData();ListTag values=tag.getList("runs",Tag.TAG_COMPOUND);for(int i=0;i<values.size();i++){DungeonRun run=DungeonRunNbtCodec.load(values.getCompound(i));data.runs.put(run.id(),run);}return data;}
}
