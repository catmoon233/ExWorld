package net.exmo.exphone;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

/** Stable identities are persisted separately from the original display names. */
record PhoneMessage(String id, String fromId, String toId, String from, String to,
                    String text, String kind, String extra, long time) {
    static PhoneMessage create(PhoneData data, String from, String to, String text, String kind, String extra) {
        return new PhoneMessage(UUID.randomUUID().toString(), account(data, from), account(data, to),
                from, to, text, kind, extra, System.currentTimeMillis());
    }

    static PhoneMessage legacy(PhoneData data, String raw, int index) {
        String[] parts = raw.split("\t", 5);
        String from = parts.length > 0 ? parts[0] : "";
        String to = parts.length > 1 ? parts[1] : "";
        return new PhoneMessage("legacy-" + index, account(data, from), account(data, to), from, to,
                parts.length > 2 ? parts[2] : raw, parts.length > 3 ? parts[3] : "text",
                parts.length > 4 ? parts[4] : "", 0);
    }

    private static String account(PhoneData data, String name) {
        if (name.startsWith("g:")) return name;
        PhoneData.Profile profile = data.byWechat(name);
        return profile == null ? "" : profile.id;
    }

    String fromName(PhoneData data) { return name(data, fromId, from); }
    String toName(PhoneData data) { return name(data, toId, to); }
    private static String name(PhoneData data, String id, String fallback) {
        PhoneData.Profile p = data.profiles.get(id);
        return p == null ? fallback : p.wechat;
    }

    boolean visible(PhoneData data, PhoneData.Profile self) {
        if (fromId.equals(self.id) || toId.equals(self.id)) return true;
        if (toId.startsWith("g:")) return data.groups.stream().anyMatch(g -> ("g:" + g.id).equals(toId) && g.has(self.wechat));
        return false; // Unresolved legacy identities must never grant access by a reused display name.
    }

    String conversation(PhoneData.Profile self) {
        if (toId.startsWith("g:")) return toId;
        return fromId.equals(self.id) ? toId : fromId;
    }

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("id", id); t.putString("fromId", fromId); t.putString("toId", toId);
        t.putString("from", from); t.putString("to", to); t.putString("text", text);
        t.putString("kind", kind); t.putString("extra", extra); t.putLong("time", time);
        return t;
    }

    static PhoneMessage load(CompoundTag t) {
        return new PhoneMessage(t.getString("id"), t.getString("fromId"), t.getString("toId"),
                t.getString("from"), t.getString("to"), t.getString("text"), t.getString("kind"), t.getString("extra"), t.getLong("time"));
    }
}
