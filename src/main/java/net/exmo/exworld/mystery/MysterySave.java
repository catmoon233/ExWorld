package net.exmo.exworld.mystery;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** One server-owned save. Knowledge survives rewind; transient state belongs to a run id. */
public final class MysterySave extends SavedData {
    public static final String ID = "exworld_mystery";
    public static final Factory<MysterySave> FACTORY = new Factory<>(MysterySave::new, MysterySave::load);

    public GamePhase phase = GamePhase.LOBBY;
    public UUID runId = UUID.randomUUID();
    public long tick;
    public long rewindAt = -1;
    public long endingAt = -1;
    public long timelineDayTime;
    public int cycle;
    public int sealCount;
    public boolean ritualPerformed;
    public final Map<UUID, Participant> players = new LinkedHashMap<>();
    public final Set<String> priestMemory = new LinkedHashSet<>();
    public final Map<UUID, ActiveCue> activeCues = new LinkedHashMap<>();
    public ActiveCue globalCue;

    public record ActiveCue(UUID run, String id, long start, int duration) {}

    public static MysterySave get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public Participant participant(UUID id) { return players.get(id); }
    public Collection<Participant> participants() { return players.values(); }
    public void changed() { setDirty(); }

    public static final class Participant {
        public final UUID uuid;
        public String characterId;
        public String displayName;
        public String role = "commoner";
        public Era era;
        public PlayerState state = PlayerState.READY;
        public final Set<String> knowledge = new LinkedHashSet<>();
        public String dimension = "minecraft:overworld";
        public double x, y, z;
        public float yaw, pitch;
        public long lastClueTick;
        public long lastWorkTick = -1000;
        public int hintStage;
        public int suspicion;
        public boolean needsRestore;
        public final Set<String> cycleContacts = new LinkedHashSet<>();

        public Participant(UUID uuid, String characterId, String displayName, Era era) {
            this.uuid = uuid;
            this.characterId = characterId;
            this.displayName = displayName;
            this.era = era;
        }

        public boolean alive() {
            return state == PlayerState.PAST_ACTIVE || state == PlayerState.FUTURE_ACTIVE
                    || state == PlayerState.FUTURE_DOOMED || state == PlayerState.BOARDED
                    || state == PlayerState.ENDING_CHASE;
        }
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("layout_version", 1);
        tag.putString("phase", phase.name());
        tag.putUUID("run", runId);
        tag.putLong("tick", tick);
        tag.putLong("rewind_at", rewindAt);
        tag.putLong("ending_at", endingAt);
        tag.putLong("timeline_day_time", timelineDayTime);
        tag.putInt("cycle", cycle);
        tag.putInt("seals", sealCount);
        tag.putBoolean("ritual_performed", ritualPerformed);
        ListTag memory = new ListTag(); priestMemory.forEach(value -> memory.add(StringTag.valueOf(value)));
        tag.put("priest_memory", memory);
        ListTag cues = new ListTag();
        activeCues.forEach((player, cue) -> {
            CompoundTag item = new CompoundTag(); item.putUUID("player", player); item.putUUID("run", cue.run());
            item.putString("id", cue.id()); item.putLong("start", cue.start()); item.putInt("duration", cue.duration());
            cues.add(item);
        });
        tag.put("active_cues", cues);
        if (globalCue != null) {
            CompoundTag item = new CompoundTag(); item.putUUID("run", globalCue.run());
            item.putString("id", globalCue.id()); item.putLong("start", globalCue.start());
            item.putInt("duration", globalCue.duration()); tag.put("global_cue", item);
        }
        ListTag participants = new ListTag();
        players.values().forEach(p -> {
            CompoundTag item = new CompoundTag();
            item.putUUID("uuid", p.uuid);
            item.putString("character", p.characterId);
            item.putString("display", p.displayName);
            item.putString("role", p.role);
            item.putString("era", p.era.name());
            item.putString("state", p.state.name());
            item.putString("dimension", p.dimension);
            item.putDouble("x", p.x); item.putDouble("y", p.y); item.putDouble("z", p.z);
            item.putFloat("yaw", p.yaw); item.putFloat("pitch", p.pitch);
            item.putLong("last_clue", p.lastClueTick);
            item.putLong("last_work", p.lastWorkTick);
            item.putInt("hint_stage", p.hintStage);
            item.putInt("suspicion", p.suspicion);
            item.putBoolean("needs_restore", p.needsRestore);
            ListTag contacts = new ListTag(); p.cycleContacts.forEach(value -> contacts.add(StringTag.valueOf(value)));
            item.put("contacts", contacts);
            ListTag clues = new ListTag(); p.knowledge.forEach(value -> clues.add(StringTag.valueOf(value)));
            item.put("knowledge", clues);
            participants.add(item);
        });
        tag.put("players", participants);
        return tag;
    }

    private static MysterySave load(CompoundTag tag, HolderLookup.Provider registries) {
        MysterySave data = new MysterySave();
        data.phase = parse(GamePhase.class, tag.getString("phase"), GamePhase.LOBBY);
        if (tag.hasUUID("run")) data.runId = tag.getUUID("run");
        data.tick = tag.getLong("tick");
        data.rewindAt = tag.contains("rewind_at") ? tag.getLong("rewind_at") : -1;
        data.endingAt = tag.contains("ending_at") ? tag.getLong("ending_at") : -1;
        data.timelineDayTime = tag.getLong("timeline_day_time");
        data.cycle = tag.getInt("cycle");
        data.sealCount = tag.getInt("seals");
        data.ritualPerformed = tag.getBoolean("ritual_performed");
        readStrings(tag.getList("priest_memory", Tag.TAG_STRING), data.priestMemory);
        ListTag cues = tag.getList("active_cues", Tag.TAG_COMPOUND);
        for (int i = 0; i < cues.size(); i++) {
            CompoundTag item = cues.getCompound(i);
            if (item.hasUUID("player") && item.hasUUID("run"))
                data.activeCues.put(item.getUUID("player"), new ActiveCue(item.getUUID("run"),
                        item.getString("id"), item.getLong("start"), item.getInt("duration")));
        }
        CompoundTag global = tag.getCompound("global_cue");
        if (global.hasUUID("run")) data.globalCue = new ActiveCue(global.getUUID("run"),
                global.getString("id"), global.getLong("start"), global.getInt("duration"));
        ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag item = players.getCompound(i);
            if (!item.hasUUID("uuid")) continue;
            UUID id = item.getUUID("uuid");
            Participant p = new Participant(id, item.getString("character"), item.getString("display"),
                    parse(Era.class, item.getString("era"), Era.NONE));
            p.role = item.getString("role");
            p.state = parse(PlayerState.class, item.getString("state"), PlayerState.READY);
            p.dimension = item.getString("dimension");
            p.x = item.getDouble("x"); p.y = item.getDouble("y"); p.z = item.getDouble("z");
            p.yaw = item.getFloat("yaw"); p.pitch = item.getFloat("pitch");
            p.lastClueTick = item.getLong("last_clue"); p.suspicion = item.getInt("suspicion");
            p.lastWorkTick = item.contains("last_work") ? item.getLong("last_work") : -1000;
            p.hintStage = item.getInt("hint_stage");
            p.needsRestore = item.getBoolean("needs_restore");
            readStrings(item.getList("contacts", Tag.TAG_STRING), p.cycleContacts);
            readStrings(item.getList("knowledge", Tag.TAG_STRING), p.knowledge);
            data.players.put(id, p);
        }
        return data;
    }

    private static void readStrings(ListTag tags, Set<String> out) {
        for (int i = 0; i < tags.size(); i++) out.add(tags.getString(i));
    }

    private static <E extends Enum<E>> E parse(Class<E> kind, String name, E fallback) {
        try { return Enum.valueOf(kind, name); } catch (RuntimeException ignored) { return fallback; }
    }
}
