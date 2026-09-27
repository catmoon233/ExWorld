package net.exmo.exworld.mystery;

import net.exmo.exworld.mystery.MysterySave.Participant;
import net.exmo.exworld.mystery.blueprint.BlueprintGraph;

import java.util.List;
import java.util.UUID;

/** Meaningful rules at the two error-prone edges: many-to-many fate and graph publication. */
public final class MysteryRulesHarness {
    public static void main(String[] args) {
        Participant past = p("adam", Era.PAST), futureA = p("adam", Era.FUTURE), futureB = p("adam", Era.FUTURE);
        Participant unrelated = p("eve", Era.FUTURE);
        check(MysteryRules.futureVictims(past, List.of(past, futureA, futureB, unrelated)).size() == 2,
                "one past identity dooms every matching future identity");
        futureA.state = PlayerState.FUTURE_DEAD;
        check(MysteryRules.futureVictims(past, List.of(past, futureA, futureB, unrelated)).equals(List.of(futureB)),
                "dead futures are not affected twice");
        futureB.state = PlayerState.ENDING_CHASE;
        check(MysteryRules.futureVictims(past, List.of(past, futureA, futureB)).equals(List.of(futureB)),
                "a past death still propagates during the train chase");
        check(MysteryRules.outcome(7).equals("bad") && MysteryRules.outcome(8).equals("normal")
                && MysteryRules.outcome(11).equals("normal") && MysteryRules.outcome(12).equals("perfect"),
                "ending intervals cover every boundary");
        String valid = "{\"id\":\"exworld:test\",\"version\":1,\"nodes\":["
                + "{\"id\":\"start\",\"type\":\"trigger\",\"params\":{\"kind\":\"on_phase_enter\"}},"
                + "{\"id\":\"wait\",\"type\":\"wait\",\"params\":{\"ticks\":20}},"
                + "{\"id\":\"cue\",\"type\":\"cue\",\"params\":{\"id\":\"rewind\",\"duration\":60}}],"
                + "\"edges\":[{\"from\":\"start\",\"to\":\"wait\"},{\"from\":\"wait\",\"to\":\"cue\"}]}";
        BlueprintGraph graph = BlueprintGraph.parse(valid);
        check(graph.next("wait", "next").size() == 1, "wait resumes from its typed next port");
        boolean rejected = false;
        try { BlueprintGraph.parse(valid.replace("\"to\":\"cue\"", "\"to\":\"start\"")); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "edges into triggers are rejected before publication");
        String incomplete = valid.replace(",{\"from\":\"wait\",\"to\":\"cue\"}", "");
        check(BlueprintGraph.parseDraft(incomplete).nodes().size() == 3, "incomplete drafts remain editable");
        rejected = false;
        try { BlueprintGraph.parse(incomplete); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "unreachable draft nodes cannot be published");
        String duplicated = valid.replace("{\"from\":\"wait\",\"to\":\"cue\"}",
                "{\"from\":\"wait\",\"to\":\"cue\"},{\"from\":\"wait\",\"to\":\"cue\"}");
        rejected = false;
        try { BlueprintGraph.parse(duplicated); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "duplicate outgoing edges cannot duplicate actions");
        String unsafe = valid.replace("\"type\":\"cue\",\"params\":{\"id\":\"rewind\",\"duration\":60}",
                "\"type\":\"action\",\"params\":{\"kind\":\"run_function\",\"value\":\"minecraft:kill_all\"}");
        rejected = false;
        try { BlueprintGraph.parse(unsafe); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "unsafe function actions cannot be published");
        System.out.println("MYSTERY_RULES_OK");
    }

    private static Participant p(String id, Era era) {
        Participant value = new Participant(UUID.randomUUID(), id, id, era);
        value.state = era == Era.PAST ? PlayerState.PAST_ACTIVE : PlayerState.FUTURE_ACTIVE;
        return value;
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
