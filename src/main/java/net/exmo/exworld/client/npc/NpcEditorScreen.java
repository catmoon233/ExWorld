package net.exmo.exworld.client.npc;

import net.exmo.exworld.npc.action.ActionRegistry;
import net.exmo.exworld.npc.data.ActionSpec;
import net.exmo.exworld.npc.data.AiSettings;
import net.exmo.exworld.npc.data.DialogBackground;
import net.exmo.exworld.npc.data.DialogScript;
import net.exmo.exworld.npc.data.MarginalBinding;
import net.exmo.exworld.npc.data.NpcCodec;
import net.exmo.exworld.npc.data.NpcDocument;
import net.exmo.exworld.npc.data.NpcLoadout;
import net.exmo.exworld.npc.data.NpcPlace;
import net.exmo.exworld.npc.data.NpcRoute;
import net.exmo.exworld.npc.data.RelationEdge;
import net.exmo.exworld.npc.data.RouteMode;
import net.exmo.exworld.npc.data.TimelineNode;
import net.exmo.exworld.npc.data.TradeSpec;
import net.exmo.exworld.npc.marginal.MarginalRegistry;
import net.exmo.exworld.npc.network.NpcPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One editor, nine pages. The server validates the whole document before replacing it. */
public final class NpcEditorScreen extends Screen {
    private static final String[] TABS = {"身份", "地点", "路线", "时间", "行动", "对话", "关系", "AI", "边际"};
    private NpcDocument doc;
    private final List<RelationEdge> relations = new ArrayList<>();
    private String error = "";
    private int tab;
    private int selected;
    private EditBox f0, f1, f2, f3, f4, f5, f6;
    private final List<String> catalogIds = new ArrayList<>();
    private final NpcCanvasEditor canvas = new NpcCanvasEditor(new NpcCanvasEditor.Host() {
        @Override public NpcDocument document() { return doc; }
        @Override public List<RelationEdge> relations() { return relations; }
        @Override public List<String> catalogIds() { return catalogIds; }
        @Override public int selected() { return selected; }
        @Override public void select(int index) { selected = index; if (f0 != null) fill(); }
        @Override public void commitFields() { commit(); }
        @Override public void moveTimeline(int index, int startMinute, int durationMinutes) { NpcEditorScreen.this.moveTimeline(index, startMinute, durationMinutes); }
        @Override public void linkDialog(int dialogIndex, int buttonIndex, String targetDialogId) { NpcEditorScreen.this.linkDialog(dialogIndex, buttonIndex, targetDialogId); }
        @Override public void linkRelation(String toId) { NpcEditorScreen.this.linkRelation(toId); }
        @Override public Font font() { return font; }
    });

    public NpcEditorScreen(CompoundTag tag) {
        super(Component.translatable("npc.exworld.editor"));
        load(tag);
    }

    public void result(boolean ok, String message) {
        error = ok ? "已保存" : (message == null || message.isBlank() ? "保存失败" : message);
    }

    private void load(CompoundTag tag) {
        doc = NpcCodec.loadDocument(tag.getCompound("document"));
        relations.clear();
        ListTag edges = tag.getList("relations", Tag.TAG_COMPOUND);
        for (int i = 0; i < edges.size(); i++) relations.add(NpcCodec.loadEdge(edges.getCompound(i)));
        error = tag.getString("error");
        catalogIds.clear();
        for (String part : tag.getString("ids").split(",")) if (!part.isBlank()) catalogIds.add(part.trim());
    }

    @Override
    protected void init() {
        int y = 40;
        for (int i = 0; i < TABS.length; i++) {
            int index = i;
            addRenderableWidget(OreButton.of(8, y, 80, 22, Component.literal(TABS[i]), b -> { commit(); tab = index; selected = 0; rebuild(); }, OreButton.Kind.SURFACE).marked(i == tab));
            y += 26;
        }
        addRenderableWidget(OreButton.of(width - 196, height - 32, 56, 22, Component.literal("蓝图"), b -> blueprint(), OreButton.Kind.SECONDARY));
        addRenderableWidget(OreButton.of(width - 132, height - 32, 56, 22, Component.literal("取消"), b -> onClose(), OreButton.Kind.SURFACE));
        addRenderableWidget(OreButton.of(width - 68, height - 32, 56, 22, Component.literal("保存"), b -> save(), OreButton.Kind.PRIMARY));
        list();
        fields();
    }

    private void blueprint() {
        commit();
        if (minecraft != null) minecraft.setScreen(new NpcBlueprintScreen(NpcBlueprintScreen.pack(doc, relations, catalogIds, error)));
    }

    private void rebuild() { clearWidgets(); init(); }

    private void list() {
        List<String> labels = labels();
        int x = 104;
        for (int i = 0; i < labels.size() && i < 10; i++) {
            int index = i;
            addRenderableWidget(OreButton.of(x, 40 + i * 22, 130, 20, Component.literal(clip(labels.get(i))), b -> { commit(); selected = index; rebuild(); }, OreButton.Kind.SURFACE).marked(i == selected));
        }
        if (tab != 0 && tab != 7) {
            addRenderableWidget(OreButton.of(x, height - 60, 60, 20, Component.literal("新增"), b -> { commit(); add(); rebuild(); }, OreButton.Kind.SECONDARY));
            addRenderableWidget(OreButton.of(x + 68, height - 60, 60, 20, Component.literal("删除"), b -> { remove(); rebuild(); }, OreButton.Kind.DANGER));
        }
        if (tab == 2) {
            addRenderableWidget(OreButton.of(x + 136, height - 60, 40, 20, Component.literal("上移"), b -> move(-1), OreButton.Kind.SURFACE));
            addRenderableWidget(OreButton.of(x + 180, height - 60, 40, 20, Component.literal("下移"), b -> move(1), OreButton.Kind.SURFACE));
        }
    }

    private void fields() {
        int x = panelX();
        int y = 36;
        f0 = box(x, y, 220);
        f1 = box(x, y + 24, 220);
        f2 = box(x, y + 48, 100);
        f3 = box(x + 108, y + 48, 112);
        f4 = box(x, y + 72, 220);
        f5 = box(x, y + 96, 220);
        f6 = box(x, y + 120, 220);
        fill();
    }

    private EditBox box(int x, int y, int width) {
        EditBox box = new EditBox(font, x, y, width, 18, Component.empty());
        box.setMaxLength(256);
        box.setBordered(false);
        box.setTextColor(OreChrome.INK);
        addRenderableWidget(box);
        return box;
    }

    private void fill() {
        switch (tab) {
            case 0 -> { f0.setValue(doc.displayName()); f1.setValue(doc.texture()); f2.setValue(doc.homePlaceId()); f3.setValue(doc.activeRouteId()); f4.setValue(doc.defaultDialogId()); f5.setValue(tradeText(doc)); f6.setValue(doc.loadout().format()); }
            case 1 -> { NpcPlace place = at(doc.places()); if (place != null) { f0.setValue(place.id()); f1.setValue(place.dimension()); f2.setValue(num(place.x())); f3.setValue(num(place.y())); f4.setValue(num(place.z())); f5.setValue(num(place.yaw())); f6.setValue(num(place.radius())); } }
            case 2 -> { NpcRoute route = at(doc.routes()); if (route != null) { f0.setValue(route.id()); f1.setValue(route.mode().name()); f2.setValue(route.loop() ? "true" : "false"); f3.setValue(num(route.speed())); f4.setValue(points(route)); } }
            case 3 -> { TimelineNode node = at(doc.timeline()); if (node != null) { f0.setValue(node.id()); f1.setValue(Integer.toString(node.startMinute())); f2.setValue(Integer.toString(node.durationMinutes())); f3.setValue(node.actionId()); f4.setValue(node.placeId()); f5.setValue(Integer.toString(node.priority())); } }
            case 4 -> { ActionSpec action = at(doc.actions()); if (action != null) { f0.setValue(action.id()); f1.setValue(action.type()); f2.setValue(String.join(",", action.children())); f4.setValue(params(action.params())); } }
            case 5 -> { DialogScript dialog = at(doc.dialogs()); if (dialog != null) { f0.setValue(dialog.id()); f1.setValue(dialog.mode().name()); f2.setValue(String.join("|", dialog.lines())); f3.setValue(dialog.fallback()); f4.setValue(Integer.toHexString(dialog.background().argb())); f5.setValue(dialog.background().texture()); f6.setValue(buttons(dialog)); } }
            case 6 -> { RelationEdge edge = at(relations); if (edge != null) { f0.setValue(edge.toId()); f1.setValue(edge.type()); f2.setValue(Integer.toString(edge.affinity())); f4.setValue(edge.note()); } }
            case 7 -> { f0.setValue(num(doc.ai().lookDistance())); f1.setValue(num(doc.ai().strollRadius())); f2.setValue(doc.ai().groundNavigation() ? "true" : "false"); f3.setValue(flags()); }
            case 8 -> { MarginalBinding binding = at(doc.marginals()); if (binding != null) { f0.setValue(binding.behaviorId()); f1.setValue(binding.enabled() ? "true" : "false"); f2.setValue(Integer.toString(binding.priority())); f3.setValue(num(binding.radius())); f4.setValue(Integer.toString(binding.durationTicks())); f5.setValue(params(binding.params())); } }
            default -> {}
        }
    }

    private void commit() {
        try {
            switch (tab) {
                case 0 -> doc = new NpcDocument(doc.id(), f0.getValue(), f1.getValue(), f2.getValue(), f3.getValue(), f4.getValue(), doc.anchored(), doc.places(), doc.routes(), doc.actions(), doc.timeline(), doc.dialogs(), doc.marginals(), parseTrades(f5.getValue()), doc.ai(), NpcLoadout.parse(f6.getValue()));
                case 1 -> replace(places());
                case 2 -> replaceRoutes();
                case 3 -> replaceNodes();
                case 4 -> replaceActions();
                case 5 -> replaceDialogs();
                case 6 -> replaceRelations();
                case 7 -> doc = new NpcDocument(doc.id(), doc.displayName(), doc.texture(), doc.homePlaceId(), doc.activeRouteId(), doc.defaultDialogId(), doc.anchored(), doc.places(), doc.routes(), doc.actions(), doc.timeline(), doc.dialogs(), doc.marginals(), doc.trades(), ai(), doc.loadout());
                case 8 -> replaceMarginals();
                default -> {}
            }
        } catch (RuntimeException ex) {
            error = "字段无法解析";
        }
    }

    private void replace(List<NpcPlace> places) { doc = doc.withPlaces(places, doc.anchored()); }
    private List<NpcPlace> places() {
        List<NpcPlace> places = new ArrayList<>(doc.places());
        if (selected < 0 || selected >= places.size()) return places;
        places.set(selected, new NpcPlace(f0.getValue(), dbl(f2), dbl(f3), dbl(f4), f1.getValue(), (float) dbl(f5), dbl(f6)));
        return places;
    }
    private void replaceRoutes() {
        List<NpcRoute> routes = new ArrayList<>(doc.routes());
        if (selected < 0 || selected >= routes.size()) return;
        NpcRoute old = routes.get(selected);
        routes.set(selected, new NpcRoute(f0.getValue(), RouteMode.parse(f1.getValue()), bool(f2), dbl(f3), parsePoints(f4.getValue(), old.points())));
        doc = doc.withRoutes(routes);
    }
    private void replaceNodes() {
        List<TimelineNode> nodes = new ArrayList<>(doc.timeline());
        if (selected < 0 || selected >= nodes.size()) return;
        nodes.set(selected, new TimelineNode(f0.getValue(), integer(f1), integer(f2), f3.getValue(), f4.getValue(), integer(f5)));
        doc = copy(doc.places(), doc.routes(), doc.actions(), nodes, doc.dialogs(), doc.marginals(), doc.trades(), doc.ai());
    }
    private void replaceActions() {
        List<ActionSpec> actions = new ArrayList<>(doc.actions());
        if (selected < 0 || selected >= actions.size()) return;
        actions.set(selected, new ActionSpec(f0.getValue(), f1.getValue(), parseParams(f4.getValue()), split(f2.getValue())));
        doc = copy(doc.places(), doc.routes(), actions, doc.timeline(), doc.dialogs(), doc.marginals(), doc.trades(), doc.ai());
    }
    private void replaceDialogs() {
        List<DialogScript> dialogs = new ArrayList<>(doc.dialogs());
        if (selected < 0 || selected >= dialogs.size()) return;
        DialogScript old = dialogs.get(selected);
        dialogs.set(selected, new DialogScript(f0.getValue(), DialogScript.DialogMode.parse(f1.getValue()), splitPipe(f2.getValue()), parseButtons(f6.getValue()),
                new DialogBackground(parseColor(f4.getValue()), old.background().alpha(), f5.getValue()), old.prompt(), f3.getValue(), old.typeSpeed()));
        doc = copy(doc.places(), doc.routes(), doc.actions(), doc.timeline(), dialogs, doc.marginals(), doc.trades(), doc.ai());
    }
    private void replaceRelations() {
        if (selected < 0 || selected >= relations.size()) return;
        RelationEdge old = relations.get(selected);
        relations.set(selected, new RelationEdge(doc.id(), f0.getValue(), f1.getValue(), integer(f2), f4.getValue()));
    }
    private void replaceMarginals() {
        List<MarginalBinding> bindings = new ArrayList<>(doc.marginals());
        if (selected < 0 || selected >= bindings.size()) return;
        bindings.set(selected, new MarginalBinding(f0.getValue(), bool(f1), integer(f2), dbl(f3), integer(f4), parseParams(f5.getValue())));
        doc = copy(doc.places(), doc.routes(), doc.actions(), doc.timeline(), doc.dialogs(), bindings, doc.trades(), doc.ai());
    }
    private AiSettings ai() {
        String flags = f3.getValue();
        return new AiSettings(dbl(f0), dbl(f1), bool(f2), flags.contains("玩家"), flags.contains("NPC"), flags.contains("门"), flags.contains("工作"));
    }
    private String flags() {
        AiSettings ai = doc.ai();
        return (ai.noticePlayers() ? "玩家 " : "") + (ai.noticeNpcs() ? "NPC " : "") + (ai.useDoors() ? "门 " : "") + (ai.faceWorkBlocks() ? "工作" : "");
    }

    private void add() {
        switch (tab) {
            case 1 -> doc = doc.withPlaces(append(doc.places(), new NpcPlace("place" + doc.places().size(), 0, 64, 0, "minecraft:overworld", 0, 1.5)), doc.anchored());
            case 2 -> doc = doc.withRoutes(append(doc.routes(), new NpcRoute("route" + doc.routes().size(), RouteMode.PATHFIND, true, 1, List.of())));
            case 3 -> doc = copy(doc.places(), doc.routes(), doc.actions(), append(doc.timeline(), new TimelineNode("node" + doc.timeline().size(), 0, 60, "", "", 0)), doc.dialogs(), doc.marginals(), doc.trades(), doc.ai());
            case 4 -> doc = copy(doc.places(), doc.routes(), append(doc.actions(), new ActionSpec("action" + doc.actions().size(), "idle", Map.of(), List.of())), doc.timeline(), doc.dialogs(), doc.marginals(), doc.trades(), doc.ai());
            case 5 -> doc = copy(doc.places(), doc.routes(), doc.actions(), doc.timeline(), append(doc.dialogs(), new DialogScript("talk" + doc.dialogs().size(), DialogScript.DialogMode.FIXED, List.of("……"), List.of(), DialogBackground.DEFAULT, "", "……", 1)), doc.marginals(), doc.trades(), doc.ai());
            case 6 -> relations.add(new RelationEdge(doc.id(), "exworld:other", "朋友", 0, ""));
            case 8 -> doc = copy(doc.places(), doc.routes(), doc.actions(), doc.timeline(), doc.dialogs(), append(doc.marginals(), new MarginalBinding("yield", false, 40, 4, 40, Map.of())), doc.trades(), doc.ai());
            default -> {}
        }
        selected = Math.max(0, labels().size() - 1);
    }

    private void remove() {
        switch (tab) {
            case 1 -> doc = doc.withPlaces(without(doc.places(), selected), doc.anchored());
            case 2 -> doc = doc.withRoutes(without(doc.routes(), selected));
            case 3 -> doc = copy(doc.places(), doc.routes(), doc.actions(), without(doc.timeline(), selected), doc.dialogs(), doc.marginals(), doc.trades(), doc.ai());
            case 4 -> doc = copy(doc.places(), doc.routes(), without(doc.actions(), selected), doc.timeline(), doc.dialogs(), doc.marginals(), doc.trades(), doc.ai());
            case 5 -> doc = copy(doc.places(), doc.routes(), doc.actions(), doc.timeline(), without(doc.dialogs(), selected), doc.marginals(), doc.trades(), doc.ai());
            case 6 -> { if (selected >= 0 && selected < relations.size()) relations.remove(selected); }
            case 8 -> doc = copy(doc.places(), doc.routes(), doc.actions(), doc.timeline(), doc.dialogs(), without(doc.marginals(), selected), doc.trades(), doc.ai());
            default -> {}
        }
        selected = Math.max(0, selected - 1);
    }

    private void move(int delta) {
        if (tab != 2) return;
        commit();
        NpcRoute route = at(doc.routes());
        if (route == null || route.points().size() < 2) return;
        int index = Math.max(0, Math.min(route.points().size() - 1, selected));
        int next = index + delta;
        if (next < 0 || next >= route.points().size()) return;
        List<NpcRoute.Waypoint> points = new ArrayList<>(route.points());
        NpcRoute.Waypoint moved = points.remove(index);
        points.add(next, moved);
        List<NpcRoute> routes = new ArrayList<>(doc.routes());
        routes.set(selected, new NpcRoute(route.id(), route.mode(), route.loop(), route.speed(), points));
        doc = doc.withRoutes(routes);
        rebuild();
    }

    private void save() {
        commit();
        CompoundTag tag = new CompoundTag();
        tag.put("document", NpcCodec.saveDocument(doc));
        ListTag edges = new ListTag();
        for (RelationEdge edge : relations) edges.add(NpcCodec.saveEdge(edge));
        tag.put("relations", edges);
        PacketDistributor.sendToServer(new NpcPayloads.Server("save", tag));
    }

    private NpcDocument copy(List<NpcPlace> places, List<NpcRoute> routes, List<ActionSpec> actions, List<TimelineNode> timeline,
                             List<DialogScript> dialogs, List<MarginalBinding> marginals, List<TradeSpec> trades, AiSettings ai) {
        return new NpcDocument(doc.id(), doc.displayName(), doc.texture(), doc.homePlaceId(), doc.activeRouteId(), doc.defaultDialogId(),
                doc.anchored(), places, routes, actions, timeline, dialogs, marginals, trades, ai, doc.loadout());
    }

    private List<String> labels() {
        return switch (tab) {
            case 1 -> doc.places().stream().map(NpcPlace::id).toList();
            case 2 -> doc.routes().stream().map(NpcRoute::id).toList();
            case 3 -> doc.timeline().stream().map(TimelineNode::id).toList();
            case 4 -> doc.actions().stream().map(ActionSpec::id).toList();
            case 5 -> doc.dialogs().stream().map(DialogScript::id).toList();
            case 6 -> relations.stream().map(edge -> edge.toId() + " " + edge.type()).toList();
            case 8 -> doc.marginals().stream().map(MarginalBinding::behaviorId).toList();
            default -> List.of(doc.id());
        };
    }

    private static <T> T at(List<T> list, int selected) { return selected >= 0 && selected < list.size() ? list.get(selected) : null; }
    private <T> T at(List<T> list) { return at(list, selected); }
    private static <T> List<T> append(List<T> list, T value) { List<T> next = new ArrayList<>(list); next.add(value); return next; }
    private static <T> List<T> without(List<T> list, int index) { List<T> next = new ArrayList<>(list); if (index >= 0 && index < next.size()) next.remove(index); return next; }
    private static String clip(String value) { return value.length() > 16 ? value.substring(0, 15) + "…" : value; }
    private static String num(double value) { return value == (long) value ? Long.toString((long) value) : Double.toString(value); }
    private static double dbl(EditBox box) { try { return Double.parseDouble(box.getValue().trim()); } catch (RuntimeException ex) { return 0; } }
    private static int integer(EditBox box) { try { return Integer.parseInt(box.getValue().trim()); } catch (RuntimeException ex) { return 0; } }
    private static boolean bool(EditBox box) { return "true".equalsIgnoreCase(box.getValue().trim()) || "1".equals(box.getValue().trim()); }
    private static List<String> split(String raw) { if (raw == null || raw.isBlank()) return List.of(); return List.of(raw.split(",")); }
    private static List<String> splitPipe(String raw) { if (raw == null || raw.isBlank()) return List.of(); return List.of(raw.split("\\|")); }
    private static String points(NpcRoute route) { return route.points().stream().map(point -> point.placeId().isBlank() ? point.x() + "," + point.y() + "," + point.z() : point.placeId()).reduce((a, b) -> a + ";" + b).orElse(""); }
    private static List<NpcRoute.Waypoint> parsePoints(String raw, List<NpcRoute.Waypoint> fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        List<NpcRoute.Waypoint> points = new ArrayList<>();
        for (String part : raw.split(";")) {
            String[] bits = part.split(",");
            if (bits.length == 1) points.add(new NpcRoute.Waypoint(bits[0].trim(), 0, 0, 0));
            else if (bits.length >= 3) points.add(new NpcRoute.Waypoint("", Double.parseDouble(bits[0]), Double.parseDouble(bits[1]), Double.parseDouble(bits[2])));
        }
        return points;
    }
    private static String params(Map<String, String> values) { return values.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).reduce((a, b) -> a + ";" + b).orElse(""); }
    private static String tradeText(NpcDocument document) {
        return document.trades().stream().map(trade -> trade.payItem() + "*" + trade.payCount() + ">" + trade.resultItem() + "*" + trade.resultCount() + "*" + trade.maxUses()).reduce((a, b) -> a + ";" + b).orElse("");
    }
    private static List<TradeSpec> parseTrades(String raw) {
        List<TradeSpec> trades = new ArrayList<>();
        if (raw == null || raw.isBlank()) return trades;
        for (String part : raw.split(";")) {
            String[] sides = part.split(">", 2);
            if (sides.length < 2) continue;
            String[] pay = sides[0].split("\\*");
            String[] result = sides[1].split("\\*");
            if (pay.length < 2 || result.length < 2) continue;
            trades.add(new TradeSpec(pay[0], Integer.parseInt(pay[1]), result[0], Integer.parseInt(result[1]), result.length > 2 ? Integer.parseInt(result[2]) : 8));
        }
        return trades;
    }
    private static Map<String, String> parseParams(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return values;
        for (String part : raw.split(";")) {
            String[] bits = part.split("=", 2);
            if (bits.length == 2) values.put(bits[0].trim(), bits[1].trim());
        }
        return values;
    }
    private static String buttons(DialogScript dialog) { return dialog.buttons().stream().map(button -> button.label() + ">" + button.dialogId() + "|" + button.actionId()).reduce((a, b) -> a + ";" + b).orElse(""); }
    private static List<DialogScript.DialogButton> parseButtons(String raw) {
        List<DialogScript.DialogButton> buttons = new ArrayList<>();
        if (raw == null || raw.isBlank()) return buttons;
        for (String part : raw.split(";")) {
            String[] label = part.split(">", 2);
            String rest = label.length > 1 ? label[1] : "";
            String[] target = rest.split("\\|", -1);
            buttons.add(new DialogScript.DialogButton(label[0], target.length > 0 ? target[0] : "", target.length > 1 ? target[1] : ""));
        }
        return buttons;
    }
    private static int parseColor(String raw) { try { return Integer.parseUnsignedInt(raw.replace("#", ""), 16); } catch (RuntimeException ex) { return 0xFF101418; } }
    private void moveTimeline(int index, int startMinute, int durationMinutes) {
        List<TimelineNode> nodes = new ArrayList<>(doc.timeline());
        if (index < 0 || index >= nodes.size()) return;
        TimelineNode old = nodes.get(index);
        nodes.set(index, new TimelineNode(old.id(), startMinute, durationMinutes, old.actionId(), old.placeId(), old.priority()));
        doc = copy(doc.places(), doc.routes(), doc.actions(), nodes, doc.dialogs(), doc.marginals(), doc.trades(), doc.ai());
        if (tab == 3 && selected == index) {
            f1.setValue(Integer.toString(nodes.get(index).startMinute()));
            f2.setValue(Integer.toString(nodes.get(index).durationMinutes()));
        }
    }

    private void linkDialog(int dialogIndex, int buttonIndex, String targetDialogId) {
        List<DialogScript> dialogs = new ArrayList<>(doc.dialogs());
        if (dialogIndex < 0 || dialogIndex >= dialogs.size()) return;
        DialogScript script = dialogs.get(dialogIndex);
        if (buttonIndex < 0 || buttonIndex >= script.buttons().size()) return;
        List<DialogScript.DialogButton> next = new ArrayList<>(script.buttons());
        DialogScript.DialogButton old = next.get(buttonIndex);
        next.set(buttonIndex, new DialogScript.DialogButton(old.label(), targetDialogId, old.actionId()));
        dialogs.set(dialogIndex, new DialogScript(script.id(), script.mode(), script.lines(), next, script.background(), script.prompt(), script.fallback(), script.typeSpeed()));
        doc = copy(doc.places(), doc.routes(), doc.actions(), doc.timeline(), dialogs, doc.marginals(), doc.trades(), doc.ai());
        if (tab == 5 && selected == dialogIndex) f6.setValue(buttons(dialogs.get(dialogIndex)));
    }

    private void linkRelation(String toId) {
        if (selected < 0 || selected >= relations.size()) {
            if (relations.isEmpty()) return;
            selected = 0;
        }
        RelationEdge old = relations.get(selected);
        relations.set(selected, new RelationEdge(old.fromId(), toId, old.type(), old.affinity(), old.note()));
        if (tab == 6 && f0 != null) f0.setValue(toId);
    }

    private boolean canvasTab() { return tab == 3 || tab == 5 || tab == 6; }
    private int panelX() { return canvasTab() ? Math.max(360, width - 248) : 248; }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OreChrome.CANVAS);
        graphics.fill(0, 0, 96, height, OreChrome.SURFACE);
        graphics.fill(95, 0, 96, height, OreChrome.EDGE);
        graphics.fill(0, height - 44, width, height, OreChrome.SURFACE);
        graphics.fill(0, height - 44, width, height - 43, OreChrome.EDGE);
        graphics.drawString(font, "NPC", 16, 14, OreChrome.GREEN, false);
        int hintX = canvasTab() ? panelX() : 248;
        OreChrome.panel(graphics, 104, 36, 136, height - 112, 0);
        if (!canvasTab()) OreChrome.panel(graphics, hintX - 8, 36, 236, height - 112, OreChrome.GREEN);
        if (canvasTab()) {
            int left = 248;
            int top = 36;
            canvas.render(graphics, left, top, Math.max(48, panelX() - 8 - left), Math.max(48, height - 56 - top), mouseX, mouseY, tab);
        }
        for (var child : children()) if (child instanceof EditBox field) OreChrome.well(graphics, field.getX(), field.getY(), field.getWidth(), field.getHeight());
        graphics.drawString(font, doc.id(), 248, 12, OreChrome.MUTED, false);
        graphics.drawString(font, hint(), hintX, canvasTab() ? height - 56 : 176, OreChrome.MUTED, false);
        if (!canvasTab()) {
            graphics.drawString(font, "行动类型 " + String.join(" ", ActionRegistry.ids()), hintX, 190, OreChrome.MUTED, false);
            graphics.drawString(font, "边际 " + String.join(" ", MarginalRegistry.ids()), hintX, 202, OreChrome.MUTED, false);
        }
        if (!error.isBlank()) graphics.drawString(font, error, 248, height - 28, error.startsWith("已") ? OreChrome.GREEN : OreChrome.RED, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (canvas.mouseClicked(mouseX, mouseY, button)) {
            setFocused(null);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (canvas.mouseDragged(mouseX, mouseY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (canvas.mouseReleased(mouseX, mouseY)) {
            rebuild();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private String hint() {
        return switch (tab) {
            case 0 -> "显示名 / 贴图 / 家 / 路线 / 默认对话 / 交易付出*数量>获得*数量*次数 / slim;kind;restock;equip;stock";
            case 1 -> "id / 维度 / x y / z / 朝向 / 半径";
            case 2 -> "id / PATHFIND|MANUAL / 循环 / 速度 / 路点(地点或x,y,z;分隔)";
            case 3 -> "id / 开始分钟 / 持续 / 行动 / 地点 / 优先级；左侧时间条拖动吸附 5 分钟，右缘改持续";
            case 4 -> "id / 类型 / 子行动,分隔 / 参数k=v;分隔";
            case 5 -> "id / FIXED|AI / 台词|分隔 / 回退 / 背景ARGB / 贴图 / 按钮 文本>对话|行动；拖按钮端口吸附到对话";
            case 6 -> "目标 / 类型(家人朋友同事对手雇主或自定义) / 好感 / 备注；从自身拖到目录 id";
            case 7 -> "注视距离 / 闲逛半径 / 地面寻路 / 过滤器：玩家 NPC 门 工作";
            case 8 -> "行为id / 启用 / 优先级 / 半径 / 持续tick / 参数";
            default -> "";
        };
    }

    @Override public boolean isPauseScreen() { return false; }
}
