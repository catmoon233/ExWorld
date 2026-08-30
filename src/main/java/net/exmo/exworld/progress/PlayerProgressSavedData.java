package net.exmo.exworld.progress;

import java.time.Instant;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Versioned single save file for all player economy, quest, history and mail state. */
public final class PlayerProgressSavedData extends SavedData {
    public static final Factory<PlayerProgressSavedData> FACTORY = new Factory<>(PlayerProgressSavedData::new, PlayerProgressSavedData::load);
    private final Map<UUID, PlayerProgress> players = new LinkedHashMap<>();
    public PlayerProgress player(UUID id) { return players.computeIfAbsent(id, unused -> new PlayerProgress()); }
    public Collection<Map.Entry<UUID, PlayerProgress>> all() { return players.entrySet(); }
    public void changed() { setDirty(); }
    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        root.putInt("version", 1); ListTag playersTag = new ListTag();
        players.forEach((id, value) -> playersTag.add(savePlayer(id, value, registries))); root.put("players", playersTag); return root;
    }
    private static CompoundTag savePlayer(UUID id, PlayerProgress value, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag(); tag.putUUID("id", id);
        ListTag resources = new ListTag(); value.resources.forEach((key, amount) -> { CompoundTag entry = new CompoundTag(); entry.putString("id", key.toString()); entry.putLong("amount", amount); resources.add(entry); }); tag.put("resources", resources);
        ListTag quests = new ListTag(); value.quests.forEach((idValue, quest) -> { CompoundTag entry = new CompoundTag(); entry.putString("id", idValue.toString()); entry.putString("node", quest.nodeId()); entry.putString("status", quest.status().name()); entry.putLong("granted", quest.grantedAt().toEpochMilli()); ListTag progress = new ListTag(); quest.progress().forEach((index, amount) -> { CompoundTag point = new CompoundTag(); point.putInt("index", index); point.putInt("amount", amount); progress.add(point); }); entry.put("progress", progress); ListTag branches = new ListTag(); quest.selectedBranches().forEach(branch -> branches.add(StringTag.valueOf(branch))); entry.put("branches", branches); quests.add(entry); }); tag.put("quests", quests);
        ListTag timeline = new ListTag(); value.timeline.forEach(entry -> { CompoundTag point = new CompoundTag(); point.putLong("at", entry.at().toEpochMilli()); point.putLong("game", entry.gameTime()); point.putString("quest", entry.questId().toString()); point.putString("type", entry.type()); point.putString("detail", entry.detail()); timeline.add(point); }); tag.put("timeline", timeline);
        ListTag mail = new ListTag(); value.mailbox.forEach(message -> { CompoundTag entry = new CompoundTag(); entry.putUUID("id", message.id()); entry.putString("source", message.source()); entry.putString("subject", message.subject()); entry.putLong("at", message.deliveredAt().toEpochMilli()); entry.putBoolean("read", message.read()); ListTag attachments = new ListTag(); message.attachments().forEach(stack -> attachments.add(stack.save(registries))); entry.put("attachments", attachments); mail.add(entry); }); tag.put("mail", mail);
        ListTag sources = new ListTag(); value.processedSources.forEach(source -> sources.add(StringTag.valueOf(source))); tag.put("sources", sources); if (value.pinnedQuest != null) tag.putString("pinned", value.pinnedQuest.toString()); return tag;
    }
    /** Public codec entry point for migration tools and persistence verification. */
    public static PlayerProgressSavedData load(CompoundTag root, HolderLookup.Provider registries) {
        PlayerProgressSavedData data = new PlayerProgressSavedData(); ListTag players = root.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) { CompoundTag tag = players.getCompound(i); PlayerProgress value = new PlayerProgress();
            for (CompoundTag entry : compounds(tag, "resources")) try { value.resources.put(ResourceLocation.parse(entry.getString("id")), Math.max(0L, entry.getLong("amount"))); } catch (Exception ignored) {}
            for (CompoundTag entry : compounds(tag, "quests")) try { QuestInstance quest = new QuestInstance(ResourceLocation.parse(entry.getString("id")), entry.getString("node"), QuestStatus.valueOf(entry.getString("status")), Instant.ofEpochMilli(entry.getLong("granted"))); for (CompoundTag point : compounds(entry, "progress")) quest.progress().put(point.getInt("index"), point.getInt("amount")); ListTag branches = entry.getList("branches", Tag.TAG_STRING); for (int j = 0; j < branches.size(); j++) quest.selectedBranches().add(branches.getString(j)); value.quests.put(quest.questId(), quest); } catch (Exception ignored) {}
            for (CompoundTag entry : compounds(tag, "timeline")) try { value.timeline.add(new QuestTimelineEntry(Instant.ofEpochMilli(entry.getLong("at")), entry.getLong("game"), ResourceLocation.parse(entry.getString("quest")), entry.getString("type"), entry.getString("detail"))); } catch (Exception ignored) {}
            for (CompoundTag entry : compounds(tag, "mail")) { List<ItemStack> attachments = new ArrayList<>(); for (CompoundTag stack : compounds(entry, "attachments")) attachments.add(ItemStack.parseOptional(registries, stack)); value.mailbox.add(new MailboxMessage(entry.getUUID("id"), entry.getString("source"), entry.getString("subject"), Instant.ofEpochMilli(entry.getLong("at")), attachments, entry.getBoolean("read"))); }
            ListTag sources = tag.getList("sources", Tag.TAG_STRING); for (int j = 0; j < sources.size(); j++) value.processedSources.add(sources.getString(j)); if (tag.contains("pinned", Tag.TAG_STRING)) try { value.pinnedQuest = ResourceLocation.parse(tag.getString("pinned")); } catch (Exception ignored) {}
            if (tag.hasUUID("id")) data.players.put(tag.getUUID("id"), value);
        } return data;
    }
    private static List<CompoundTag> compounds(CompoundTag tag, String key) { ListTag list = tag.getList(key, Tag.TAG_COMPOUND); List<CompoundTag> out = new ArrayList<>(); for (int i = 0; i < list.size(); i++) out.add(list.getCompound(i)); return out; }
}
