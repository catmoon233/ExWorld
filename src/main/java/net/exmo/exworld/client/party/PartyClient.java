package net.exmo.exworld.client.party;

import net.exmo.exworld.battle.party.PartySnapshot;

public final class PartyClient {
    private static volatile PartySnapshot snapshot = new PartySnapshot(java.util.List.of());
    private PartyClient() {}
    public static void install(PartySnapshot value){snapshot=value;}
    public static PartySnapshot snapshot(){return snapshot;}
}
