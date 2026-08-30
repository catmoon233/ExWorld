package net.exmo.exworld.battle.api;

import java.util.UUID;

public record CommandReceipt(UUID commandId, boolean accepted, String reason, long revision) {
    public static CommandReceipt accepted(UUID id, long revision) { return new CommandReceipt(id, true, "", revision); }
    public static CommandReceipt rejected(UUID id, String reason, long revision) { return new CommandReceipt(id, false, reason, revision); }
}
