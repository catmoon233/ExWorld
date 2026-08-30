package net.exmo.exworld.dungeon;

import java.util.*;

/** Deterministic 512-block slots; occupied slots are supplied by persisted runs. */
public final class DungeonSlotAllocator {
    public static final int SLOT_SIZE = 512;
    private DungeonSlotAllocator() {}
    public static Slot allocate(Collection<DungeonRun> runs) {
        Set<Long> occupied=new HashSet<>();runs.forEach(run->occupied.add(key(run.slotX(),run.slotZ())));
        for(int z=0;z<4096;z+=SLOT_SIZE)for(int x=0;x<4096;x+=SLOT_SIZE)if(!occupied.contains(key(x,z)))return new Slot(x,z);
        throw new IllegalStateException("No free dungeon instance slot");
    }
    private static long key(int x,int z){return ((long)x<<32) ^ (z&0xffffffffL);}
    public record Slot(int x,int z){}
}
