package net.exmo.exworld.battle.party;

import java.util.List;
import java.util.UUID;

public record PartySnapshot(List<Member> members) {
    public PartySnapshot { members = List.copyOf(members); }
    public record Member(UUID id, String name, boolean leader, boolean online,
                         float health, float maxHealth, float mana, float maxMana, double distance) {}
}
