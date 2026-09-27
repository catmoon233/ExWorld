package net.exmo.exphone.api;

/** A contact another mod can show on the phone's NPC card editor. */
public record PhoneNpc(String id, String name) {
    public PhoneNpc {
        id = id == null ? "" : id;
        name = name == null || name.isBlank() ? id : name;
    }
}
