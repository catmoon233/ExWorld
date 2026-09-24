package net.exmo.exworld.npc.data;

import java.util.List;
import java.util.Map;

/** Data-only sample. The runtime does not special-case this id. */
public final class NpcPresets {
    public static final String VENDOR_ID = "exworld:stall_vendor";

    private NpcPresets() {}

    public static void seed(NpcCatalog catalog) {
        if (catalog.document(VENDOR_ID).isPresent()) return;
        NpcPlace home = new NpcPlace("home", 0, 64, 0, "minecraft:overworld", 0, 1.5);
        NpcPlace stall = new NpcPlace("stall", 6, 64, 0, "minecraft:overworld", 180, 1.25);
        NpcRoute route = new NpcRoute("market", RouteMode.PATHFIND, true, 1.0, List.of(
                new NpcRoute.Waypoint("home", 0, 64, 0),
                new NpcRoute.Waypoint("stall", 6, 64, 0),
                new NpcRoute.Waypoint("home", 0, 64, 0)));
        ActionSpec commute = new ActionSpec("commute", "follow_route", Map.of("route", "market"), List.of());
        ActionSpec open = new ActionSpec("open_stall", "trade", Map.of("place", "stall"), List.of());
        ActionSpec day = new ActionSpec("day", "sequence", Map.of(), List.of("commute", "open_stall"));
        ActionSpec rest = new ActionSpec("rest", "go_home", Map.of("place", "home"), List.of());
        DialogScript shop = new DialogScript("shop", DialogScript.DialogMode.FIXED,
                List.of("要看看今天的货吗？"),
                List.of(new DialogScript.DialogButton("闲聊", "chat", ""), new DialogScript.DialogButton("交易", "", "open_stall")),
                new DialogBackground(0xFF1A2430, 0.9f, ""), "", "今天风有点大。", 1);
        DialogScript closed = new DialogScript("closed", DialogScript.DialogMode.FIXED,
                List.of("未营业，天亮再来。"), List.of(), DialogBackground.DEFAULT, "", "未营业。", 1);
        DialogScript chat = new DialogScript("chat", DialogScript.DialogMode.AI,
                List.of(), List.of(new DialogScript.DialogButton("回去", "shop", "")),
                new DialogBackground(0xFF241820, 0.88f, ""),
                "你是早市摊贩，说话短，带一点市井气。", "今天风有点大。", 1);
        NpcDocument document = new NpcDocument(VENDOR_ID, "摊贩", "", "home", "market", "shop", false,
                List.of(home, stall), List.of(route), List.of(commute, open, day, rest),
                List.of(new TimelineNode("morning", 360, 720, "day", "stall", 1),
                        new TimelineNode("night", 1080, 720, "rest", "home", 1)),
                List.of(shop, closed, chat),
                List.of(new MarginalBinding("flee_combat", true, 100, 12, 200, Map.of())),
                List.of(new TradeSpec("minecraft:emerald", 1, "minecraft:bread", 2, 8)),
                AiSettings.DEFAULT, NpcLoadout.EMPTY);
        catalog.put(document, List.of(new RelationEdge(VENDOR_ID, "exworld:guard", "同事", 20, "早市同事")));
    }
}
