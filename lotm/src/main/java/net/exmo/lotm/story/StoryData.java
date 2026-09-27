package net.exmo.lotm.story;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Letters, counterpart pairs, and the train anchor. Cycle loot and NPC claims are not stored here. */
public final class StoryData extends SavedData {
    public static final String ID = "lotm_story";
    public static final Factory<StoryData> FACTORY = new Factory<>(StoryData::new, StoryData::load);

    final Map<String, String> outbox = new LinkedHashMap<>();
    final Map<String, String> inbox = new LinkedHashMap<>();
    final Map<String, String> pairs = new LinkedHashMap<>();
    UUID letterRun;
    String dimension = "";
    double x;
    double y;
    double z;
    float yaw;
    boolean anchored;

    public static StoryData get(ServerPlayer player) {
        return player.server.overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public String letterFor(UUID id) {
        String incoming = inbox.get(id.toString());
        if (incoming != null && !incoming.isBlank()) return incoming;
        String sent = outbox.get(id.toString());
        return sent == null ? "" : sent;
    }

    public void prepareLetters(UUID run) {
        if (run == null || run.equals(letterRun)) return;
        outbox.clear(); inbox.clear(); letterRun = run; setDirty();
    }

    public void send(UUID from, String text) {
        outbox.put(from.toString(), text);
        String other = pairs.get(from.toString());
        if (other != null) inbox.put(other, text);
        setDirty();
    }

    /** Mystery's character grouping can address several future selves without changing the legacy pair map. */
    public void sendTo(UUID from, Iterable<UUID> targets, String text) {
        outbox.put(from.toString(), text);
        for (UUID target : targets) inbox.put(target.toString(), text);
        setDirty();
    }

    public UUID pairOf(UUID id) {
        String other = pairs.get(id.toString());
        if (other == null || other.isBlank()) return null;
        try {
            return UUID.fromString(other);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public void pair(UUID left, UUID right) {
        pairs.put(left.toString(), right.toString());
        pairs.put(right.toString(), left.toString());
        setDirty();
    }

    public boolean anchored() {
        return anchored && !dimension.isBlank();
    }

    public void anchor(ServerPlayer player) {
        dimension = player.level().dimension().location().toString();
        x = player.getX();
        y = player.getY();
        z = player.getZ();
        yaw = player.getYRot();
        anchored = true;
        setDirty();
    }

    public ServerLevel destination(ServerPlayer player) {
        if (!anchored()) return null;
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        if (id == null) return null;
        return player.server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id));
    }

    private static StoryData load(CompoundTag tag, HolderLookup.Provider registries) {
        StoryData data = new StoryData();
        readMap(tag.getCompound("outbox"), data.outbox);
        readMap(tag.getCompound("inbox"), data.inbox);
        readMap(tag.getCompound("pairs"), data.pairs);
        data.dimension = tag.getString("dimension");
        data.x = tag.getDouble("x");
        data.y = tag.getDouble("y");
        data.z = tag.getDouble("z");
        data.yaw = tag.getFloat("yaw");
        data.anchored = tag.getBoolean("anchored");
        if (tag.hasUUID("letter_run")) data.letterRun = tag.getUUID("letter_run");
        return data;
    }

    private static void readMap(CompoundTag tag, Map<String, String> into) {
        for (String key : tag.getAllKeys()) into.put(key, tag.getString(key));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("outbox", writeMap(outbox));
        tag.put("inbox", writeMap(inbox));
        tag.put("pairs", writeMap(pairs));
        tag.putString("dimension", dimension);
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("yaw", yaw);
        tag.putBoolean("anchored", anchored);
        if (letterRun != null) tag.putUUID("letter_run", letterRun);
        return tag;
    }

    private static CompoundTag writeMap(Map<String, String> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach(tag::putString);
        return tag;
    }
}
