package net.exmo.exworld.mystery;

import net.exmo.exworld.mystery.MysterySave.Participant;

import java.util.Collection;
import java.util.List;

/** Pure cross-era and ending rules shared by transitions and tests. */
public final class MysteryRules {
    private MysteryRules() {}

    public static List<Participant> futureVictims(Participant past, Collection<Participant> everyone) {
        if (past.era != Era.PAST) return List.of();
        return everyone.stream().filter(p -> p.era == Era.FUTURE
                && (p.state == PlayerState.FUTURE_ACTIVE || p.state == PlayerState.ENDING_CHASE)
                && p.characterId.equals(past.characterId)).toList();
    }

    public static String outcome(int seals) {
        return seals < 8 ? "bad" : seals < 12 ? "normal" : "perfect";
    }
}
