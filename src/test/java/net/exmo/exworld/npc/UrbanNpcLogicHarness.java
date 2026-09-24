package net.exmo.exworld.npc;

import net.exmo.exworld.npc.data.ActionSpec;
import net.exmo.exworld.npc.data.AiSettings;
import net.exmo.exworld.npc.data.DialogBackground;
import net.exmo.exworld.npc.data.DialogScript;
import net.exmo.exworld.npc.data.MarginalBinding;
import net.exmo.exworld.npc.data.NpcCodec;
import net.exmo.exworld.npc.data.NpcDocument;
import net.exmo.exworld.npc.data.NpcPlace;
import net.exmo.exworld.npc.data.NpcRoute;
import net.exmo.exworld.npc.data.NpcValidator;
import net.exmo.exworld.npc.data.RelationEdge;
import net.exmo.exworld.npc.data.RouteMode;
import net.exmo.exworld.npc.data.TimelineNode;
import net.exmo.exworld.npc.data.NpcLoadout;
import net.exmo.exworld.npc.skin.SkinUrls;
import net.exmo.exworld.npc.dialog.DialogResolver;
import net.exmo.exworld.npc.dialog.DialogRequest;
import net.exmo.exworld.npc.entity.NpcMovement;
import net.exmo.exworld.npc.logic.BrainContext;
import net.exmo.exworld.npc.logic.BrainDecision;
import net.exmo.exworld.npc.logic.DayClock;
import net.exmo.exworld.npc.logic.NpcBrain;
import net.exmo.exworld.npc.logic.RelationGraph;
import net.exmo.exworld.npc.logic.TimelineSelector;
import net.exmo.exworld.npc.marginal.MarginalRegistry;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UrbanNpcLogicHarness {
    private UrbanNpcLogicHarness() {}

    public static void main(String[] args) {
        check(DayClock.minuteOfDay(0) == 0, "midnight is minute 0");
        check(DayClock.minuteOfDay(24000) == 0, "next day wraps");
        check(DayClock.minuteOfDay(-1) == DayClock.minuteOfDay(23999), "negative day time wraps");
        check(DayClock.minuteOfDay(23999) <= 1439, "minute stays inside the day");

        TimelineNode night = new TimelineNode("night", 1320, 240, "rest", "home", 1);
        check(TimelineSelector.contains(night, 1400), "before midnight");
        check(TimelineSelector.contains(night, 10), "after midnight");
        check(!TimelineSelector.contains(night, 200), "outside wrapped window");
        TimelineNode overlap = new TimelineNode("late", 1380, 30, "trade", "stall", 5);
        check(TimelineSelector.select(List.of(night, overlap), 1390).orElseThrow().id().equals("late"), "higher priority wins");
        TimelineNode same = new TimelineNode("later", 1390, 20, "trade", "stall", 1);
        check(TimelineSelector.select(List.of(night, same), 1395).orElseThrow().id().equals("later"), "later start wins ties");
        check(TimelineSelector.select(List.of(), 10).isEmpty(), "empty timeline");

        BrainDecision missing = NpcBrain.tick(context(50, List.of(new TimelineNode("work", 0, 100, "gone", "", 1)),
                Map.of(), Set.of("home"), Set.of(), "", "", 0, false));
        check("missing-action".equals(missing.idleReason()), "missing action idles");
        BrainDecision missingPlace = NpcBrain.tick(context(50, List.of(new TimelineNode("work", 0, 100, "post", "missing", 1)),
                Map.of("post", new ActionSpec("post", "hold_post", Map.of(), List.of())), Set.of("home"), Set.of(), "", "", 0, false));
        check("missing-place".equals(missingPlace.idleReason()), "missing place idles");
        BrainDecision skipped = NpcBrain.tick(context(50, List.of(new TimelineNode("work", 0, 100, "combo", "", 1)),
                Map.of("combo", new ActionSpec("combo", "sequence", Map.of(), List.of("nope", "rest")),
                        "rest", new ActionSpec("rest", "idle", Map.of(), List.of())),
                Set.of(), Set.of(), "", "", 0, false));
        check("idle".equals(skipped.actionType()) && "rest".equals(skipped.actionId()), "unknown child is skipped");

        MarginalBinding flee = new MarginalBinding("flee_combat", true, 100, 12, 40, Map.of());
        BrainDecision preempt = NpcBrain.tick(context(600, List.of(new TimelineNode("work", 0, 800, "rest", "", 1)),
                Map.of("rest", new ActionSpec("rest", "idle", Map.of(), List.of())), Set.of("home"), Set.of(), "work", "", 10, true)
                .withMarginals(List.of(flee)));
        check(preempt.kind() == BrainDecision.Kind.MARGINAL && "flee_combat".equals(preempt.marginalId()), "combat preempts");
        check("work".equals(preempt.suspendedNodeId()), "suspended node is remembered");
        BrainContext expired = context(600, List.of(new TimelineNode("work", 0, 800, "rest", "", 1)),
                Map.of("rest", new ActionSpec("rest", "idle", Map.of(), List.of())), Set.of("home"), Set.of(), "work", "flee_combat", 5, false)
                .withMarginals(List.of(flee)).withSuspended("work");
        BrainDecision resumed = NpcBrain.tick(expired);
        check(resumed.kind() == BrainDecision.Kind.RESUME && "work".equals(resumed.nodeId()) && resumed.marginalId().isBlank(), "expiry restores the node");

        RelationGraph graph = new RelationGraph(List.of(new RelationEdge("a", "b", "朋友", 10, "")));
        check(graph.find("a", "missing").isEmpty(), "missing npc is empty");
        check(graph.find(null, "b").isEmpty(), "null endpoint is empty");

        check(NpcMovement.nextIndex(1, 3, true, true) == 2, "manual advance");
        check(NpcMovement.nextIndex(2, 3, true, true) == 0, "manual loop");
        check(NpcMovement.shouldYield(NpcMovement.stuckAfter(7, true)), "stuck path yields");
        check(!NpcMovement.shouldYield(NpcMovement.stuckAfter(1, false)), "a successful step clears the stuck count");

        DialogScript fixed = new DialogScript("shop", DialogScript.DialogMode.FIXED, List.of("要看看今天的货吗？"), List.of(), DialogBackground.DEFAULT, "", "回退", 1);
        check("要看看今天的货吗？".equals(DialogResolver.resolve(fixed, null, new DialogRequest("摊贩", "", "", "玩家", List.of()))), "fixed line");
        DialogScript ai = new DialogScript("chat", DialogScript.DialogMode.AI, List.of(), List.of(), DialogBackground.DEFAULT, "prompt", "回退", 1);
        check("回退".equals(DialogResolver.resolve(ai, request -> { throw new IllegalStateException("down"); }, new DialogRequest("摊贩", "", "", "玩家", List.of()))), "ai failure falls back");

        NpcDocument document = sample();
        RelationEdge edge = new RelationEdge(document.id(), "exworld:guard", "同事", 20, "早市");
        NpcDocument loaded = NpcCodec.loadDocument(NpcCodec.saveDocument(document));
        RelationEdge loadedEdge = NpcCodec.loadEdge(NpcCodec.saveEdge(edge));
        check(loaded.equals(document), "document round trip");
        check(loadedEdge.equals(edge), "relation round trip");
        check(NpcValidator.validate(document, List.of(edge)) == null, "preset validates");
        check(NpcValidator.validate(new NpcDocument(document.id(), document.displayName(), "", "home", "market", "shop", true,
                document.places(), document.routes(), document.actions(), document.timeline(), document.dialogs(),
                List.of(new MarginalBinding("not_a_behavior", true, 1, 1, 1, Map.of())), document.trades(), AiSettings.DEFAULT, NpcLoadout.EMPTY),
                List.of()).startsWith("unknown marginal"), "unknown marginal is rejected");
        check(!MarginalRegistry.known("not_a_behavior"), "unknown marginal is ignored by the registry");
        NpcLoadout loadout = NpcLoadout.parse("slim=true;kind=url;restock=15;equip=head=minecraft:leather_helmet;stock=minecraft:bread*8");
        check(loadout.slim() && "url".equals(loadout.skinKind()) && loadout.restockMinutes() == 15, "loadout fields");
        check(loadout.equipment().size() == 1 && "head".equals(loadout.equipment().getFirst().slot()), "loadout equipment");
        check(loadout.inventory().size() == 1 && loadout.inventory().getFirst().count() == 8, "loadout stock");
        check(NpcLoadout.parse(loadout.format()).equals(loadout), "loadout round trip");
        check(NpcLoadout.parse(NpcLoadout.EMPTY.format()).equals(NpcLoadout.EMPTY), "empty loadout round trip");
        check(SkinUrls.reject("file:///tmp/skin.png") != null, "file skin rejected");
        check(SkinUrls.reject("http://localhost/skin.png") != null, "localhost skin rejected");
        check(SkinUrls.reject("https://user:pass@example.com/skin.png") != null, "userinfo skin rejected");
        check(SkinUrls.reject("https://textures.minecraft.net/texture/skin.png") == null, "public https skin accepted");
         NpcDocument copied = sample().withId("exworld:copy", false);
         check("exworld:copy".equals(copied.id()) && !copied.anchored() && copied.places().size() == sample().places().size(), "copy keeps places and drops the anchor");
         check("market".equals(copied.activeRouteId()) && copied.routes().getFirst().points().size() == 1, "copy keeps the route");
         NpcRoute grown = copied.routes().getFirst().appendPlace("stall").appendPlace("stall");
         check(grown.points().size() == 2 && "stall".equals(grown.points().get(1).placeId()), "place waypoint appends once");
         check(grown.points().getFirst().equals(copied.routes().getFirst().points().getFirst()), "append does not rewrite earlier points");
         check(grown.dropLast().points().size() == 1 && grown.dropLast().points().getFirst().placeId().equals("home"), "undo drops only the last point");
         check(copied.withActiveRoute("market").activeRouteId().equals("market"), "active route can be selected");
        System.out.println("UrbanNpcLogicHarness ok");
    }

    private static BrainContext context(int minute, List<TimelineNode> nodes, Map<String, ActionSpec> actions, Set<String> places,
                                        Set<String> routes, String node, String marginal, long tick, boolean combat) {
        return new BrainContext(minute, tick, false, combat, false, false, false, false, 0, false, false, false, 0,
                node, "", marginal, tick, 0, nodes, List.of(), actions, places, routes, places.contains("home") ? "home" : "", Set.of());
    }

    private static NpcDocument sample() {
        return new NpcDocument("exworld:stall_vendor", "摊贩", "", "home", "market", "shop", true,
                List.of(new NpcPlace("home", 1, 64, 2, "minecraft:overworld", 0, 1.5),
                        new NpcPlace("stall", 6, 64, 2, "minecraft:overworld", 180, 1.25)),
                List.of(new NpcRoute("market", RouteMode.MANUAL, true, 1, List.of(new NpcRoute.Waypoint("home", 0, 0, 0)))),
                List.of(new ActionSpec("rest", "idle", Map.of(), List.of())),
                List.of(new TimelineNode("morning", 360, 720, "rest", "", 1)),
                List.of(new DialogScript("shop", DialogScript.DialogMode.FIXED, List.of("你好"), List.of(), DialogBackground.DEFAULT, "", "回退", 1)),
                List.of(new MarginalBinding("flee_combat", true, 100, 12, 40, Map.of())),
                List.of(), AiSettings.DEFAULT, NpcLoadout.EMPTY);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
