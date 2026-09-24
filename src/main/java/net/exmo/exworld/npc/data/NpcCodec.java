package net.exmo.exworld.npc.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** NBT adapter kept out of the behavior model. Unknown marginal ids are dropped on load. */
public final class NpcCodec {
    private NpcCodec() {}

    public static CompoundTag saveDocument(NpcDocument doc) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", doc.id());
        tag.putString("name", doc.displayName());
        tag.putString("texture", doc.texture());
        tag.putString("home", doc.homePlaceId());
        tag.putString("route", doc.activeRouteId());
        tag.putString("dialog", doc.defaultDialogId());
        tag.putBoolean("anchored", doc.anchored());
        tag.put("places", places(doc.places()));
        tag.put("routes", routes(doc.routes()));
        tag.put("actions", actions(doc.actions()));
        tag.put("timeline", timeline(doc.timeline()));
        tag.put("dialogs", dialogs(doc.dialogs()));
        tag.put("marginals", marginals(doc.marginals()));
        tag.put("trades", trades(doc.trades()));
        tag.put("ai", ai(doc.ai()));
        tag.put("loadout", loadout(doc.loadout()));
        return tag;
    }

    public static NpcDocument loadDocument(CompoundTag tag) {
        return new NpcDocument(
                tag.getString("id"), tag.getString("name"), tag.getString("texture"),
                tag.getString("home"), tag.getString("route"), tag.getString("dialog"), tag.getBoolean("anchored"),
                loadPlaces(tag.getList("places", Tag.TAG_COMPOUND)),
                loadRoutes(tag.getList("routes", Tag.TAG_COMPOUND)),
                loadActions(tag.getList("actions", Tag.TAG_COMPOUND)),
                loadTimeline(tag.getList("timeline", Tag.TAG_COMPOUND)),
                loadDialogs(tag.getList("dialogs", Tag.TAG_COMPOUND)),
                loadMarginals(tag.getList("marginals", Tag.TAG_COMPOUND)),
                loadTrades(tag.getList("trades", Tag.TAG_COMPOUND)),
                loadAi(tag.getCompound("ai")), loadLoadout(tag.getCompound("loadout")));
    }

    public static CompoundTag saveEdge(RelationEdge edge) {
        CompoundTag tag = new CompoundTag();
        tag.putString("from", edge.fromId());
        tag.putString("to", edge.toId());
        tag.putString("type", edge.type());
        tag.putInt("affinity", edge.affinity());
        tag.putString("note", edge.note());
        return tag;
    }

    public static RelationEdge loadEdge(CompoundTag tag) {
        return new RelationEdge(tag.getString("from"), tag.getString("to"), tag.getString("type"), tag.getInt("affinity"), tag.getString("note"));
    }

    private static ListTag places(List<NpcPlace> places) {
        ListTag list = new ListTag();
        for (NpcPlace place : places) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", place.id());
            tag.putDouble("x", place.x());
            tag.putDouble("y", place.y());
            tag.putDouble("z", place.z());
            tag.putString("dim", place.dimension());
            tag.putFloat("yaw", place.yaw());
            tag.putDouble("r", place.radius());
            list.add(tag);
        }
        return list;
    }

    private static List<NpcPlace> loadPlaces(ListTag list) {
        List<NpcPlace> places = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            places.add(new NpcPlace(tag.getString("id"), tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"),
                    tag.getString("dim"), tag.getFloat("yaw"), tag.contains("r") ? tag.getDouble("r") : 1.5));
        }
        return places;
    }

    private static ListTag routes(List<NpcRoute> routes) {
        ListTag list = new ListTag();
        for (NpcRoute route : routes) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", route.id());
            tag.putString("mode", route.mode().name());
            tag.putBoolean("loop", route.loop());
            tag.putDouble("speed", route.speed());
            ListTag points = new ListTag();
            for (NpcRoute.Waypoint point : route.points()) {
                CompoundTag pointTag = new CompoundTag();
                pointTag.putString("place", point.placeId());
                pointTag.putDouble("x", point.x());
                pointTag.putDouble("y", point.y());
                pointTag.putDouble("z", point.z());
                points.add(pointTag);
            }
            tag.put("points", points);
            list.add(tag);
        }
        return list;
    }

    private static List<NpcRoute> loadRoutes(ListTag list) {
        List<NpcRoute> routes = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ListTag points = tag.getList("points", Tag.TAG_COMPOUND);
            List<NpcRoute.Waypoint> waypoints = new ArrayList<>();
            for (int p = 0; p < points.size(); p++) {
                CompoundTag point = points.getCompound(p);
                waypoints.add(new NpcRoute.Waypoint(point.getString("place"), point.getDouble("x"), point.getDouble("y"), point.getDouble("z")));
            }
            routes.add(new NpcRoute(tag.getString("id"), RouteMode.parse(tag.getString("mode")), tag.getBoolean("loop"), tag.getDouble("speed"), waypoints));
        }
        return routes;
    }

    private static ListTag actions(List<ActionSpec> actions) {
        ListTag list = new ListTag();
        for (ActionSpec action : actions) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", action.id());
            tag.putString("type", action.type());
            tag.put("params", map(action.params()));
            tag.put("children", strings(action.children()));
            list.add(tag);
        }
        return list;
    }

    private static List<ActionSpec> loadActions(ListTag list) {
        List<ActionSpec> actions = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            actions.add(new ActionSpec(tag.getString("id"), tag.getString("type"), loadMap(tag.getList("params", Tag.TAG_COMPOUND)), loadStrings(tag.getList("children", Tag.TAG_STRING))));
        }
        return actions;
    }

    private static ListTag timeline(List<TimelineNode> nodes) {
        ListTag list = new ListTag();
        for (TimelineNode node : nodes) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", node.id());
            tag.putInt("start", node.startMinute());
            tag.putInt("duration", node.durationMinutes());
            tag.putString("action", node.actionId());
            tag.putString("place", node.placeId());
            tag.putInt("priority", node.priority());
            list.add(tag);
        }
        return list;
    }

    private static List<TimelineNode> loadTimeline(ListTag list) {
        List<TimelineNode> nodes = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            nodes.add(new TimelineNode(tag.getString("id"), tag.getInt("start"), tag.getInt("duration"), tag.getString("action"), tag.getString("place"), tag.getInt("priority")));
        }
        return nodes;
    }

    private static ListTag dialogs(List<DialogScript> dialogs) {
        ListTag list = new ListTag();
        for (DialogScript dialog : dialogs) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", dialog.id());
            tag.putString("mode", dialog.mode().name());
            tag.put("lines", strings(dialog.lines()));
            ListTag buttons = new ListTag();
            for (DialogScript.DialogButton button : dialog.buttons()) {
                CompoundTag buttonTag = new CompoundTag();
                buttonTag.putString("label", button.label());
                buttonTag.putString("dialog", button.dialogId());
                buttonTag.putString("action", button.actionId());
                buttons.add(buttonTag);
            }
            tag.put("buttons", buttons);
            CompoundTag background = new CompoundTag();
            background.putInt("argb", dialog.background().argb());
            background.putFloat("alpha", dialog.background().alpha());
            background.putString("texture", dialog.background().texture());
            tag.put("background", background);
            tag.putString("prompt", dialog.prompt());
            tag.putString("fallback", dialog.fallback());
            tag.putInt("speed", dialog.typeSpeed());
            list.add(tag);
        }
        return list;
    }

    private static List<DialogScript> loadDialogs(ListTag list) {
        List<DialogScript> dialogs = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            List<DialogScript.DialogButton> buttons = new ArrayList<>();
            ListTag buttonTags = tag.getList("buttons", Tag.TAG_COMPOUND);
            for (int b = 0; b < buttonTags.size(); b++) {
                CompoundTag button = buttonTags.getCompound(b);
                buttons.add(new DialogScript.DialogButton(button.getString("label"), button.getString("dialog"), button.getString("action")));
            }
            CompoundTag background = tag.getCompound("background");
            dialogs.add(new DialogScript(tag.getString("id"), DialogScript.DialogMode.parse(tag.getString("mode")),
                    loadStrings(tag.getList("lines", Tag.TAG_STRING)), buttons,
                    new DialogBackground(background.getInt("argb"), background.contains("alpha") ? background.getFloat("alpha") : 0.86f, background.getString("texture")),
                    tag.getString("prompt"), tag.getString("fallback"), tag.contains("speed") ? tag.getInt("speed") : 1));
        }
        return dialogs;
    }

    private static ListTag marginals(List<MarginalBinding> bindings) {
        ListTag list = new ListTag();
        for (MarginalBinding binding : bindings) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", binding.behaviorId());
            tag.putBoolean("enabled", binding.enabled());
            tag.putInt("priority", binding.priority());
            tag.putDouble("radius", binding.radius());
            tag.putInt("ticks", binding.durationTicks());
            tag.put("params", map(binding.params()));
            list.add(tag);
        }
        return list;
    }

    private static List<MarginalBinding> loadMarginals(ListTag list) {
        List<MarginalBinding> bindings = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            String id = tag.getString("id");
            if (!net.exmo.exworld.npc.marginal.MarginalRegistry.known(id)) continue;
            bindings.add(new MarginalBinding(id, tag.getBoolean("enabled"), tag.getInt("priority"), tag.getDouble("radius"), tag.getInt("ticks"), loadMap(tag.getList("params", Tag.TAG_COMPOUND))));
        }
        return bindings;
    }

    private static ListTag trades(List<TradeSpec> trades) {
        ListTag list = new ListTag();
        for (TradeSpec trade : trades) {
            CompoundTag tag = new CompoundTag();
            tag.putString("pay", trade.payItem());
            tag.putInt("payCount", trade.payCount());
            tag.putString("result", trade.resultItem());
            tag.putInt("resultCount", trade.resultCount());
            tag.putInt("uses", trade.maxUses());
            list.add(tag);
        }
        return list;
    }

    private static List<TradeSpec> loadTrades(ListTag list) {
        List<TradeSpec> trades = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            trades.add(new TradeSpec(tag.getString("pay"), tag.getInt("payCount"), tag.getString("result"), tag.getInt("resultCount"), tag.getInt("uses")));
        }
        return trades;
    }

    private static CompoundTag ai(AiSettings settings) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("look", settings.lookDistance());
        tag.putDouble("stroll", settings.strollRadius());
        tag.putBoolean("ground", settings.groundNavigation());
        tag.putBoolean("players", settings.noticePlayers());
        tag.putBoolean("npcs", settings.noticeNpcs());
        tag.putBoolean("doors", settings.useDoors());
        tag.putBoolean("work", settings.faceWorkBlocks());
        return tag;
    }

    private static AiSettings loadAi(CompoundTag tag) {
        if (tag.isEmpty()) return AiSettings.DEFAULT;
        return new AiSettings(tag.getDouble("look"), tag.getDouble("stroll"), !tag.contains("ground") || tag.getBoolean("ground"),
                tag.getBoolean("players"), tag.getBoolean("npcs"), tag.getBoolean("doors"), tag.getBoolean("work"));
    }

    private static ListTag map(Map<String, String> values) {
        ListTag list = new ListTag();
        values.forEach((key, value) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("k", key);
            tag.putString("v", value == null ? "" : value);
            list.add(tag);
        });
        return list;
    }

    private static Map<String, String> loadMap(ListTag list) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            values.put(tag.getString("k"), tag.getString("v"));
        }
        return values;
    }

    private static ListTag strings(List<String> values) {
        ListTag list = new ListTag();
        for (String value : values) list.add(StringTag.valueOf(value == null ? "" : value));
        return list;
    }

    private static List<String> loadStrings(ListTag list) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) values.add(list.getString(i));
        return values;
    }

    private static CompoundTag loadout(NpcLoadout loadout) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("slim", loadout.slim());
        tag.putString("kind", loadout.skinKind());
        tag.putInt("restock", loadout.restockMinutes());
        ListTag gear = new ListTag();
        for (NpcLoadout.Gear piece : loadout.equipment()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("slot", piece.slot());
            entry.putString("item", piece.item());
            gear.add(entry);
        }
        tag.put("equip", gear);
        ListTag stock = new ListTag();
        for (NpcLoadout.Stock line : loadout.inventory()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("item", line.item());
            entry.putInt("count", line.count());
            stock.add(entry);
        }
        tag.put("stock", stock);
        return tag;
    }

    private static NpcLoadout loadLoadout(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return NpcLoadout.EMPTY;
        java.util.List<NpcLoadout.Gear> gear = new java.util.ArrayList<>();
        ListTag equip = tag.getList("equip", Tag.TAG_COMPOUND);
        for (int i = 0; i < equip.size(); i++) {
            CompoundTag entry = equip.getCompound(i);
            gear.add(new NpcLoadout.Gear(entry.getString("slot"), entry.getString("item")));
        }
        java.util.List<NpcLoadout.Stock> stock = new java.util.ArrayList<>();
        ListTag lines = tag.getList("stock", Tag.TAG_COMPOUND);
        for (int i = 0; i < lines.size(); i++) {
            CompoundTag entry = lines.getCompound(i);
            stock.add(new NpcLoadout.Stock(entry.getString("item"), entry.getInt("count")));
        }
        return new NpcLoadout(tag.getBoolean("slim"), tag.getString("kind"), tag.getInt("restock"), gear, stock);
    }
}