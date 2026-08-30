package net.exmo.exworld.progress;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Aggregate owned by one player: inventory-independent rewards and their story state. */
public final class PlayerProgress {
    final Map<ResourceLocation, Long> resources = new LinkedHashMap<>();
    final Map<ResourceLocation, QuestInstance> quests = new LinkedHashMap<>();
    final List<QuestTimelineEntry> timeline = new ArrayList<>();
    final List<MailboxMessage> mailbox = new ArrayList<>();
    final Set<String> processedSources = new LinkedHashSet<>();
    ResourceLocation pinnedQuest;
}
