package net.exmo.exworld.battle.party;

import java.util.*;

/** Runtime-only debug party aggregate. All membership mutations pass through this small interface. */
public final class DebugPartyManager {
    public static final int MAX_MEMBERS = 4;
    public static final long INVITE_LIFETIME_TICKS = 20L * 30;
    private final Map<UUID, Party> parties = new LinkedHashMap<>();
    private final Map<UUID, UUID> partyByMember = new HashMap<>();
    private final Map<UUID, Invitation> invitations = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();

    public Result invite(UUID inviter, String inviterName, UUID target, String targetName, long now) {
        names.put(inviter, inviterName); names.put(target, targetName);
        expire(now);
        if (inviter.equals(target)) return Result.SELF;
        if (sameParty(inviter, target)) return Result.ALREADY_MEMBER;
        if (partyByMember.containsKey(target)) return Result.TARGET_IN_PARTY;
        Party party = party(inviter);
        if (!party.leader.equals(inviter)) return Result.NOT_LEADER;
        if (party.members.size() >= MAX_MEMBERS) return Result.FULL;
        invitations.put(target, new Invitation(party.id, inviter, now + INVITE_LIFETIME_TICKS));
        return Result.OK;
    }

    public Result accept(UUID target, long now) {
        expire(now);
        Invitation invitation = invitations.remove(target);
        if (invitation == null) return Result.NO_INVITE;
        if (partyByMember.containsKey(target)) return Result.TARGET_IN_PARTY;
        Party party = parties.get(invitation.partyId);
        if (party == null || party.members.size() >= MAX_MEMBERS) return party == null ? Result.NO_INVITE : Result.FULL;
        party.members.add(target); partyByMember.put(target, party.id);
        return Result.OK;
    }

    public Result decline(UUID target) { return invitations.remove(target) == null ? Result.NO_INVITE : Result.OK; }

    public Result leave(UUID member) {
        Party party = partyOf(member);
        if (party == null || party.members.size() <= 1) { removeSolo(member); return Result.NOT_IN_PARTY; }
        party.members.remove(member); partyByMember.remove(member); invitations.entrySet().removeIf(entry -> entry.getValue().inviter.equals(member));
        if (party.leader.equals(member)) party.leader = party.members.getFirst();
        collapse(party); return Result.OK;
    }

    public Result kick(UUID leader, UUID target) {
        Party party = partyOf(leader);
        if (party == null || !party.leader.equals(leader)) return Result.NOT_LEADER;
        if (leader.equals(target)) return Result.SELF;
        if (!party.members.remove(target)) return Result.NOT_MEMBER;
        partyByMember.remove(target); collapse(party); return Result.OK;
    }

    public List<UUID> members(UUID member) {
        Party party = partyOf(member);
        return party == null ? List.of(member) : List.copyOf(party.members);
    }
    public UUID leader(UUID member) { Party party = partyOf(member); return party == null ? member : party.leader; }
    public String name(UUID member) { return names.getOrDefault(member, member.toString().substring(0, 8)); }
    public boolean sameParty(UUID first, UUID second) { UUID party = partyByMember.get(first); return party != null && party.equals(partyByMember.get(second)); }
    public Optional<InvitationView> invitation(UUID target, long now) {
        expire(now); Invitation value = invitations.get(target);
        return value == null ? Optional.empty() : Optional.of(new InvitationView(value.inviter, name(value.inviter), value.expiresAt));
    }
    public void clear() { parties.clear(); partyByMember.clear(); invitations.clear(); names.clear(); }

    private Party party(UUID member) {
        Party existing = partyOf(member); if (existing != null) return existing;
        UUID id = UUID.randomUUID(); Party created = new Party(id, member, new ArrayList<>(List.of(member)));
        parties.put(id, created); partyByMember.put(member, id); return created;
    }
    private Party partyOf(UUID member) { UUID id = partyByMember.get(member); return id == null ? null : parties.get(id); }
    private void expire(long now) { invitations.entrySet().removeIf(entry -> entry.getValue().expiresAt < now); }
    private void collapse(Party party) {
        if (party.members.size() > 1) return;
        parties.remove(party.id); party.members.forEach(partyByMember::remove);
    }
    private void removeSolo(UUID member) { UUID id = partyByMember.remove(member); if (id != null) parties.remove(id); }

    public enum Result { OK, SELF, ALREADY_MEMBER, TARGET_IN_PARTY, NOT_LEADER, FULL, NO_INVITE, NOT_IN_PARTY, NOT_MEMBER }
    public record InvitationView(UUID inviter, String inviterName, long expiresAt) {}
    private record Invitation(UUID partyId, UUID inviter, long expiresAt) {}
    private static final class Party {
        private final UUID id; private UUID leader; private final ArrayList<UUID> members;
        private Party(UUID id, UUID leader, ArrayList<UUID> members) { this.id=id;this.leader=leader;this.members=members; }
    }
}
