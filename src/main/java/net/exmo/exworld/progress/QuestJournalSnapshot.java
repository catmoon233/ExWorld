package net.exmo.exworld.progress;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Full server projection required by the journal book. */
public record QuestJournalSnapshot(List<QuestSnapshot> quests, List<QuestTimelineEntry> timeline, List<Mail> mail,
                                  Map<ResourceLocation, Long> resources, ResourceLocation pinnedQuest) {
    public QuestJournalSnapshot { quests=List.copyOf(quests); timeline=List.copyOf(timeline); mail=List.copyOf(mail); resources=Map.copyOf(resources); }
    public record Mail(UUID id, String source, String subject, long deliveredAt, boolean read, List<ItemStack> attachments) { public Mail { attachments=attachments.stream().map(ItemStack::copy).toList(); } }
}
