package net.exmo.lotm.phone;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class PhoneData extends SavedData {
    public static final String ID = "lotm_phone";
    public static final Factory<PhoneData> FACTORY = new Factory<>(PhoneData::new, PhoneData::load);
    final Map<String, Profile> profiles = new LinkedHashMap<>();
    final List<String> messages = new ArrayList<>();
    final List<Sms> sms = new ArrayList<>();
    final List<Moment> moments = new ArrayList<>();
    final List<Group> groups = new ArrayList<>();
    final List<Packet> packets = new ArrayList<>();
    final Map<String, Sim> sims = new LinkedHashMap<>();
    long momentSeq;
    long groupSeq;
    long packetSeq;

    public static PhoneData get(ServerPlayer player) {
        return player.server.overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public String numberOf(ServerPlayer player) {
        Profile profile = profiles.get(player.getUUID().toString());
        return profile == null ? "" : profile.number;
    }

    public String freshNumber() {
        for (int i = 0; i < 24; i++) {
            long body = ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L);
            String number = "1" + (3 + ThreadLocalRandom.current().nextInt(7)) + body;
            if (byNumber(number) == null) return number;
        }
        return "";
    }

    public String install(ServerPlayer player, String number, boolean replace) {
        if (number.isBlank()) return "无法分配号码";
        Profile self = ensure(player);
        if (!self.number.isBlank() && !replace) return "已有手机号";
        Profile owner = byNumber(number);
        if (owner != null && !owner.id.equals(self.id)) return "号码已被占用";
        self.number = number;
        setDirty();
        return "";
    }

    Profile ensure(ServerPlayer player) {
        Profile profile = profiles.computeIfAbsent(player.getUUID().toString(), Profile::player);
        if (profile.wechat.isBlank()) {
            String name = player.getGameProfile().getName();
            profile.wechat = byWechat(name) == null ? name : name + player.getUUID().toString().substring(0, 4);
            setDirty();
        }
        return profile;
    }

    Profile npc(String id) {
        return profiles.computeIfAbsent("npc:" + id, key -> Profile.npc(key));
    }

    Profile byWechat(String name) {
        if (name == null || name.isBlank()) return null;
        for (Profile profile : profiles.values()) {
            if (name.equalsIgnoreCase(profile.wechat)) return profile;
        }
        return null;
    }

    Profile byNumber(String number) {
        if (number == null || number.isBlank()) return null;
        for (Profile profile : profiles.values()) {
            if (number.equals(profile.number)) return profile;
        }
        return null;
    }
    Sim sim(String number) {
        return sims.computeIfAbsent(number, Sim::of);
    }

    Sim balance(String number) {
        return number == null || number.isBlank() ? null : sims.get(number);
    }


    void touch() {
        setDirty();
    }

    private static PhoneData load(CompoundTag tag, HolderLookup.Provider registries) {
        PhoneData data = new PhoneData();
        data.momentSeq = tag.getLong("momentSeq");
        ListTag profiles = tag.getList("profiles", Tag.TAG_COMPOUND);
        for (int i = 0; i < profiles.size(); i++) {
            Profile profile = Profile.load(profiles.getCompound(i));
            if (!profile.id.isBlank()) data.profiles.put(profile.id, profile);
        }
        ListTag messages = tag.getList("messages", Tag.TAG_STRING);
        for (int i = 0; i < messages.size(); i++) data.messages.add(messages.getString(i));
        ListTag sms = tag.getList("sms", Tag.TAG_COMPOUND);
        for (int i = 0; i < sms.size(); i++) data.sms.add(Sms.load(sms.getCompound(i)));
        ListTag moments = tag.getList("moments", Tag.TAG_COMPOUND);
        for (int i = 0; i < moments.size(); i++) data.moments.add(Moment.load(moments.getCompound(i)));
        data.groupSeq = tag.getLong("groupSeq");
        data.packetSeq = tag.getLong("packetSeq");
        ListTag groups = tag.getList("groups", Tag.TAG_COMPOUND);
        for (int i = 0; i < groups.size(); i++) data.groups.add(Group.load(groups.getCompound(i)));
        ListTag packets = tag.getList("packets", Tag.TAG_COMPOUND);
        for (int i = 0; i < packets.size(); i++) data.packets.add(Packet.load(packets.getCompound(i)));
        ListTag sims = tag.getList("sims", Tag.TAG_COMPOUND);
        for (int i = 0; i < sims.size(); i++) {
            Sim sim = Sim.load(sims.getCompound(i));
            if (!sim.number.isBlank()) data.sims.put(sim.number, sim);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("momentSeq", momentSeq);
        ListTag profiles = new ListTag();
        for (Profile profile : this.profiles.values()) profiles.add(profile.save());
        tag.put("profiles", profiles);
        ListTag messages = new ListTag();
        for (String message : this.messages) messages.add(StringTag.valueOf(message));
        tag.put("messages", messages);
        ListTag sms = new ListTag();
        for (Sms line : this.sms) sms.add(line.save());
        tag.put("sms", sms);
        ListTag moments = new ListTag();
        for (Moment moment : this.moments) moments.add(moment.save());
        tag.put("moments", moments);
        tag.putLong("groupSeq", groupSeq);
        tag.putLong("packetSeq", packetSeq);
        ListTag groups = new ListTag();
        for (Group group : this.groups) groups.add(group.save());
        tag.put("groups", groups);
        ListTag packets = new ListTag();
        for (Packet packet : this.packets) packets.add(packet.save());
        tag.put("packets", packets);
        ListTag sims = new ListTag();
        for (Sim sim : this.sims.values()) sims.add(sim.save());
        tag.put("sims", sims);
        return tag;
    }

    static final class Profile {
        String id;
        String number = "";
        String wechat = "";
        final List<String> friends = new ArrayList<>();
        String wifiDim = "";
        int wifiX;
        int wifiY;
        int wifiZ;
        String wifiPass = "";
        String theme = "light";
        String font = "14";
        int noteSeq;
        final List<String> notes = new ArrayList<>();
        final List<String> requests = new ArrayList<>();

        static Profile player(String id) {
            Profile profile = new Profile();
            profile.id = id;
            return profile;
        }

        static Profile npc(String id) {
            Profile profile = new Profile();
            profile.id = id;
            return profile;
        }

        boolean npc() {
            return id.startsWith("npc:");
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", id);
            tag.putString("number", number);
            tag.putString("wechat", wechat);
            tag.putString("wifiDim", wifiDim);
            tag.putInt("wifiX", wifiX);
            tag.putInt("wifiY", wifiY);
            tag.putInt("wifiZ", wifiZ);
            tag.putString("wifiPass", wifiPass);
            tag.putString("theme", theme);
            tag.putString("font", font);
            tag.putInt("noteSeq", noteSeq);
            tag.put("notes", strings(notes));
            tag.put("friends", strings(friends));
            tag.put("requests", strings(requests));
            return tag;
        }

        static Profile load(CompoundTag tag) {
            Profile profile = new Profile();
            profile.id = tag.getString("id");
            profile.number = tag.getString("number");
            profile.wechat = tag.getString("wechat");
            profile.wifiDim = tag.getString("wifiDim");
            profile.wifiX = tag.getInt("wifiX");
            profile.wifiY = tag.getInt("wifiY");
            profile.wifiZ = tag.getInt("wifiZ");
            profile.wifiPass = tag.getString("wifiPass");
            profile.theme = tag.getString("theme").isBlank() ? "light" : tag.getString("theme");
            profile.font = tag.getString("font").isBlank() ? "14" : tag.getString("font");
            profile.noteSeq = tag.getInt("noteSeq");
            read(tag.getList("notes", Tag.TAG_STRING), profile.notes);
            read(tag.getList("friends", Tag.TAG_STRING), profile.friends);
            read(tag.getList("requests", Tag.TAG_STRING), profile.requests);
            return profile;
        }
    }

    static final class Sms {
        String from = "";
        String to = "";
        String text = "";

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("from", from);
            tag.putString("to", to);
            tag.putString("text", text);
            return tag;
        }

        static Sms load(CompoundTag tag) {
            Sms sms = new Sms();
            sms.from = tag.getString("from");
            sms.to = tag.getString("to");
            sms.text = tag.getString("text");
            return sms;
        }
    }

    static final class Moment {
        String id = "";
        String author = "";
        String text = "";
        final List<String> likes = new ArrayList<>();
        final List<String> comments = new ArrayList<>();

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", id);
            tag.putString("author", author);
            tag.putString("text", text);
            tag.put("likes", strings(likes));
            tag.put("comments", strings(comments));
            return tag;
        }

        static Moment load(CompoundTag tag) {
            Moment moment = new Moment();
            moment.id = tag.getString("id");
            moment.author = tag.getString("author");
            moment.text = tag.getString("text");
            read(tag.getList("likes", Tag.TAG_STRING), moment.likes);
            read(tag.getList("comments", Tag.TAG_STRING), moment.comments);
            return moment;
        }
    }

    private static ListTag strings(List<String> values) {
        ListTag list = new ListTag();
        for (String value : values) list.add(StringTag.valueOf(value));
        return list;
    }

    private static void read(ListTag list, List<String> into) {
        for (int i = 0; i < list.size(); i++) into.add(list.getString(i));
    }

    static final class Group {
        String id = "";
        String name = "";
        String owner = "";
        final List<String> members = new ArrayList<>();

        boolean has(String wechat) {
            return members.stream().anyMatch(name -> name.equalsIgnoreCase(wechat));
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", id);
            tag.putString("name", name);
            tag.putString("owner", owner);
            tag.put("members", strings(members));
            return tag;
        }

        static Group load(CompoundTag tag) {
            Group group = new Group();
            group.id = tag.getString("id");
            group.name = tag.getString("name");
            group.owner = tag.getString("owner");
            read(tag.getList("members", Tag.TAG_STRING), group.members);
            return group;
        }
    }

    static final class Packet {
        String id = "";
        String kind = "";
        String from = "";
        String target = "";
        int amount;
        int shares = 1;
        int left;
        final List<String> claimed = new ArrayList<>();

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", id);
            tag.putString("kind", kind);
            tag.putString("from", from);
            tag.putString("target", target);
            tag.putInt("amount", amount);
            tag.putInt("shares", shares);
            tag.putInt("left", left);
            tag.put("claimed", strings(claimed));
            return tag;
        }

        static Packet load(CompoundTag tag) {
            Packet packet = new Packet();
            packet.id = tag.getString("id");
            packet.kind = tag.getString("kind");
            packet.from = tag.getString("from");
            packet.target = tag.getString("target");
            packet.amount = tag.getInt("amount");
            packet.shares = Math.max(1, tag.getInt("shares"));
            packet.left = tag.getInt("left");
            read(tag.getList("claimed", Tag.TAG_STRING), packet.claimed);
            return packet;
        }
    }
    static final class Sim {
        String number = "";
        int credit;
        int data;

        static Sim of(String number) {
            Sim sim = new Sim();
            sim.number = number;
            return sim;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("number", number);
            tag.putInt("credit", credit);
            tag.putInt("data", data);
            return tag;
        }

        static Sim load(CompoundTag tag) {
            Sim sim = new Sim();
            sim.number = tag.getString("number");
            sim.credit = tag.getInt("credit");
            sim.data = tag.getInt("data");
            return sim;
        }
    }
}
