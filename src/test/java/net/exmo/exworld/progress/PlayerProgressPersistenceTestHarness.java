package net.exmo.exworld.progress;

import java.time.Instant;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Verifies the aggregate preserves long balances, audit events, branches and source IDs through NBT. */
public final class PlayerProgressPersistenceTestHarness {
    public static void main(String[] args) {
        PlayerProgressSavedData data=new PlayerProgressSavedData();UUID player=UUID.randomUUID();PlayerProgress state=data.player(player);ResourceLocation quest=ResourceLocation.fromNamespaceAndPath("test","journey"),gold=PlayerResourceVault.GOLD;state.resources.put(gold,4_000_000_000L);QuestInstance instance=new QuestInstance(quest,"node",QuestStatus.ACTIVE,Instant.EPOCH);instance.progress().put(0,2);instance.selectedBranches().add("node->other");state.quests.put(quest,instance);state.timeline.add(new QuestTimelineEntry(Instant.EPOCH,42,quest,"granted","test"));state.processedSources.add("resource:test");
        HolderLookup.Provider lookup=net.minecraft.core.HolderLookup.Provider.create(java.util.stream.Stream.of());CompoundTag tag=data.save(new CompoundTag(),lookup);PlayerProgress restored=PlayerProgressSavedData.load(tag,lookup).player(player);check(restored.resources.get(gold)==4_000_000_000L,"long resource amount survives");check(restored.quests.get(quest).selectedBranches().contains("node->other"),"branch history survives");check(restored.timeline.size()==1&&restored.processedSources.contains("resource:test"),"audit and idempotency survive");System.out.println("PLAYER_PROGRESS_PERSISTENCE_TEST_OK");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
