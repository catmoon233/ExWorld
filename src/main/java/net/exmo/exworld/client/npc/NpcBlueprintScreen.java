package net.exmo.exworld.client.npc;

import net.exmo.exworld.npc.data.ActionSpec;
import net.exmo.exworld.npc.data.DialogScript;
import net.exmo.exworld.npc.data.NpcCodec;
import net.exmo.exworld.npc.data.NpcDocument;
import net.exmo.exworld.npc.data.NpcPlace;
import net.exmo.exworld.npc.data.NpcRoute;
import net.exmo.exworld.npc.data.RelationEdge;
import net.exmo.exworld.npc.data.RouteMode;
import net.exmo.exworld.npc.data.TimelineNode;
import net.exmo.exworld.npc.network.NpcPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

/** Node blueprint for one NPC document. Layout is local; links write real ids. */
public final class NpcBlueprintScreen extends Screen {
    private NpcDocument doc;
    private final List<RelationEdge> relations = new ArrayList<>();
    private final List<String> catalogIds = new ArrayList<>();
    private final Map<String, float[]> at = new LinkedHashMap<>();
    private String error = "";
    private String kind = "";
    private int selected = -1;
    private int drag;
    private int dragIndex = -1;
    private int dragButton = -1;
    private double originX;
    private double originY;
    private float originNodeX;
    private float originNodeY;
    private EditBox f0;
    private EditBox f1;
    private EditBox f2;

    public NpcBlueprintScreen(CompoundTag tag) {
        super(Component.translatable("npc.exworld.blueprint"));
        load(tag);
    }

    public void result(boolean ok, String message) {
        error = ok ? "已保存" : (message == null || message.isBlank() ? "保存失败" : message);
    }

    public static CompoundTag pack(NpcDocument document, List<RelationEdge> edges, List<String> ids, String error) {
        CompoundTag tag = new CompoundTag();
        tag.put("document", NpcCodec.saveDocument(document));
        ListTag list = new ListTag();
        for (RelationEdge edge : edges) list.add(NpcCodec.saveEdge(edge));
        tag.put("relations", list);
        tag.putString("ids", String.join(",", ids));
        tag.putString("error", error == null ? "" : error);
        return tag;
    }

    private void load(CompoundTag tag) {
        doc = NpcCodec.loadDocument(tag.getCompound("document"));
        relations.clear();
        ListTag edges = tag.getList("relations", Tag.TAG_COMPOUND);
        for (int i = 0; i < edges.size(); i++) relations.add(NpcCodec.loadEdge(edges.getCompound(i)));
        catalogIds.clear();
        for (String part : tag.getString("ids").split(",")) if (!part.isBlank()) catalogIds.add(part.trim());
        error = tag.getString("error");
    }

    @Override
    protected void init() {
        addRenderableWidget(OreButton.of(width - 248, 6, 56, 18, Component.literal("保存"), b -> save(), OreButton.Kind.PRIMARY));
        addRenderableWidget(OreButton.of(width - 188, 6, 56, 18, Component.literal("表单"), b -> form(), OreButton.Kind.SURFACE));
        addRenderableWidget(OreButton.of(width - 68, 6, 56, 18, Component.literal("关闭"), b -> onClose(), OreButton.Kind.DANGER));
        int x = width - 210;
        addRenderableWidget(OreButton.of(x, 136, 92, 18, Component.literal("新增地点"), b -> addNode("place"), OreButton.Kind.SECONDARY));
        addRenderableWidget(OreButton.of(x + 96, 136, 92, 18, Component.literal("新增行动"), b -> addNode("action"), OreButton.Kind.SECONDARY));
        addRenderableWidget(OreButton.of(x, 158, 92, 18, Component.literal("新增时间"), b -> addNode("time"), OreButton.Kind.SECONDARY));
        addRenderableWidget(OreButton.of(x + 96, 158, 92, 18, Component.literal("新增对话"), b -> addNode("dialog"), OreButton.Kind.SECONDARY));
        addRenderableWidget(OreButton.of(x, 180, 92, 18, Component.literal("新增路线"), b -> addNode("route"), OreButton.Kind.SECONDARY));
        addRenderableWidget(OreButton.of(x + 96, 180, 92, 18, Component.literal("设为当前"), b -> activateRoute(), OreButton.Kind.PRIMARY));
        addRenderableWidget(OreButton.of(x, 202, 188, 18, Component.literal("删除选中"), b -> deleteSelected(), OreButton.Kind.DANGER));
        f0 = field(x, 40, 190);
        f1 = field(x, 64, 190);
        f2 = field(x, 88, 190);
        fill();
    }

    private void addNode(String nodeKind) {
        commitFields();
        switch (nodeKind) {
            case "place" -> {
                List<NpcPlace> places = new ArrayList<>(doc.places());
                places.add(new NpcPlace("place" + places.size(), 0, 64, 0, "minecraft:overworld", 0, 1.5));
                doc = doc.withPlaces(places, doc.anchored());
                selected = places.size() - 1;
            }
            case "action" -> {
                List<ActionSpec> actions = new ArrayList<>(doc.actions());
                actions.add(new ActionSpec("action" + actions.size(), "idle", Map.of(), List.of()));
                doc = copy(doc.places(), actions, doc.timeline(), doc.dialogs());
                selected = actions.size() - 1;
            }
            case "time" -> {
                List<TimelineNode> nodes = new ArrayList<>(doc.timeline());
                nodes.add(new TimelineNode("node" + nodes.size(), 0, 60, "", "", 0));
                doc = copy(doc.places(), doc.actions(), nodes, doc.dialogs());
                selected = nodes.size() - 1;
            }
            case "route" -> {
                List<NpcRoute> routes = new ArrayList<>(doc.routes());
                String id = "path" + routes.size();
                routes.add(new NpcRoute(id, RouteMode.PATHFIND, true, 1, List.of()));
                doc = doc.withRoutes(routes);
                if (doc.activeRouteId().isBlank()) doc = doc.withActiveRoute(id);
                selected = routes.size() - 1;
            }
            default -> {
                List<DialogScript> dialogs = new ArrayList<>(doc.dialogs());
                dialogs.add(new DialogScript("talk" + dialogs.size(), DialogScript.DialogMode.FIXED, List.of("……"), List.of(),
                        net.exmo.exworld.npc.data.DialogBackground.DEFAULT, "", "……", 1));
                doc = copy(doc.places(), doc.actions(), doc.timeline(), dialogs);
                selected = dialogs.size() - 1;
            }
        }
        kind = nodeKind;
        fill();
    }

    private void deleteSelected() {
        if (selected < 0) return;
        commitFields();
        if ("place".equals(kind) && selected < doc.places().size()) {
            NpcPlace place = doc.places().get(selected);
            if (place.id().equals(doc.homePlaceId())) { error = "不能删除家"; return; }
            List<NpcPlace> places = new ArrayList<>(doc.places());
            places.remove(selected);
            doc = doc.withPlaces(places, doc.anchored());
        } else if ("action".equals(kind) && selected < doc.actions().size()) {
            List<ActionSpec> actions = new ArrayList<>(doc.actions());
            actions.remove(selected);
            doc = copy(doc.places(), actions, doc.timeline(), doc.dialogs());
        } else if ("time".equals(kind) && selected < doc.timeline().size()) {
            List<TimelineNode> nodes = new ArrayList<>(doc.timeline());
            nodes.remove(selected);
            doc = copy(doc.places(), doc.actions(), nodes, doc.dialogs());
        } else if ("dialog".equals(kind) && selected < doc.dialogs().size()) {
            List<DialogScript> dialogs = new ArrayList<>(doc.dialogs());
            dialogs.remove(selected);
            doc = copy(doc.places(), doc.actions(), doc.timeline(), dialogs);
        } else if ("route".equals(kind) && selected < doc.routes().size()) {
            String removed = doc.routes().get(selected).id();
            List<NpcRoute> routes = new ArrayList<>(doc.routes());
            routes.remove(selected);
            doc = doc.withRoutes(routes);
            if (removed.equals(doc.activeRouteId())) doc = doc.withActiveRoute("");
        }
        selected = -1;
        kind = "";
        fill();
    }

    private void activateRoute() {
        commitFields();
        if (!"route".equals(kind) || selected < 0 || selected >= doc.routes().size()) {
            error = "先选一条路线";
            return;
        }
        doc = doc.withActiveRoute(doc.routes().get(selected).id());
        error = "已设为当前路线";
    }

    private EditBox field(int x, int y, int w) {
        EditBox box = new EditBox(font, x, y, w, 18, Component.empty());
        box.setMaxLength(128);
        box.setBordered(false);
        box.setTextColor(OreChrome.INK);
        addRenderableWidget(box);
        return box;
    }

    private void fill() {
        f0.setValue("");
        f1.setValue("");
        f2.setValue("");
        if ("place".equals(kind)) {
            NpcPlace place = at(doc.places(), selected);
            if (place != null) f0.setValue(place.id());
        } else if ("action".equals(kind)) {
            ActionSpec action = at(doc.actions(), selected);
            if (action != null) { f0.setValue(action.id()); f1.setValue(action.type()); }
        } else if ("time".equals(kind)) {
            TimelineNode node = at(doc.timeline(), selected);
            if (node != null) { f0.setValue(node.id()); f1.setValue(node.actionId()); f2.setValue(node.placeId()); }
        } else if ("dialog".equals(kind)) {
            DialogScript dialog = at(doc.dialogs(), selected);
            if (dialog != null) f0.setValue(dialog.id());
        } else if ("route".equals(kind)) {
            NpcRoute route = at(doc.routes(), selected);
            if (route != null) {
                f0.setValue(route.id());
                f1.setValue(route.mode().name().toLowerCase(java.util.Locale.ROOT));
                f2.setValue(Double.toString(route.speed()));
            }
        }
    }

    private void commitFields() {
        try {
            if ("place".equals(kind) && selected >= 0 && selected < doc.places().size()) {
                List<NpcPlace> places = new ArrayList<>(doc.places());
                NpcPlace old = places.get(selected);
                places.set(selected, new NpcPlace(f0.getValue(), old.x(), old.y(), old.z(), old.dimension(), old.yaw(), old.radius()));
                doc = doc.withPlaces(places, doc.anchored());
            } else if ("action".equals(kind) && selected >= 0 && selected < doc.actions().size()) {
                List<ActionSpec> actions = new ArrayList<>(doc.actions());
                ActionSpec old = actions.get(selected);
                actions.set(selected, new ActionSpec(f0.getValue(), f1.getValue(), old.params(), old.children()));
                doc = copy(doc.places(), actions, doc.timeline(), doc.dialogs());
            } else if ("time".equals(kind) && selected >= 0 && selected < doc.timeline().size()) {
                List<TimelineNode> nodes = new ArrayList<>(doc.timeline());
                TimelineNode old = nodes.get(selected);
                nodes.set(selected, new TimelineNode(f0.getValue(), old.startMinute(), old.durationMinutes(), f1.getValue(), f2.getValue(), old.priority()));
                doc = copy(doc.places(), doc.actions(), nodes, doc.dialogs());
            } else if ("dialog".equals(kind) && selected >= 0 && selected < doc.dialogs().size()) {
                List<DialogScript> dialogs = new ArrayList<>(doc.dialogs());
                DialogScript old = dialogs.get(selected);
                dialogs.set(selected, new DialogScript(f0.getValue(), old.mode(), old.lines(), old.buttons(), old.background(), old.prompt(), old.fallback(), old.typeSpeed()));
                doc = copy(doc.places(), doc.actions(), doc.timeline(), dialogs);
            } else if ("route".equals(kind) && selected >= 0 && selected < doc.routes().size()) {
                List<NpcRoute> routes = new ArrayList<>(doc.routes());
                NpcRoute old = routes.get(selected);
                String id = f0.getValue() == null || f0.getValue().isBlank() ? old.id() : f0.getValue().trim();
                double speed = old.speed();
                try {
                    if (f2.getValue() != null && !f2.getValue().isBlank()) speed = Double.parseDouble(f2.getValue().trim());
                } catch (NumberFormatException ignored) {
                    error = "速度无法解析";
                }
                routes.set(selected, new NpcRoute(id, RouteMode.parse(f1.getValue()), old.loop(), speed, old.points()));
                doc = doc.withRoutes(routes);
                if (old.id().equals(doc.activeRouteId())) doc = doc.withActiveRoute(id);
            }
        } catch (RuntimeException ex) {
            error = "字段无法解析";
        }
    }

    private void save() {
        commitFields();
        CompoundTag tag = new CompoundTag();
        tag.put("document", NpcCodec.saveDocument(doc));
        ListTag edges = new ListTag();
        for (RelationEdge edge : relations) edges.add(NpcCodec.saveEdge(edge));
        tag.put("relations", edges);
        PacketDistributor.sendToServer(new NpcPayloads.Server("save", tag));
    }

    private void form() {
        commitFields();
        Minecraft.getInstance().setScreen(new NpcEditorScreen(pack(doc, relations, catalogIds, error)));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OreChrome.CANVAS);
        graphics.fill(0, 0, width, 28, OreChrome.DEEP);
        graphics.fill(0, 26, width, 28, OreChrome.EDGE);
        OreChrome.panel(graphics, 8, 32, Math.max(80, width - 236), height - 40, OreChrome.GOLD);
        OreChrome.panel(graphics, width - 220, 32, 208, height - 40, OreChrome.PURPLE);
        drawLinks(graphics);
        drawNodes(graphics);
        if (drag == 2) line(graphics, (int) originX, (int) originY, mouseX, mouseY, OreChrome.GOLD);
        graphics.drawString(font, doc.displayName(), 12, 8, OreChrome.GOLD, false);
        if (!error.isBlank()) graphics.drawString(font, clip(error), 150, 8, error.startsWith("已") ? OreChrome.GREEN : OreChrome.RED, false);
        graphics.drawString(font, "地点 行动 时间 对话 路线", 12, height - 18, OreChrome.MUTED, false);
        graphics.drawString(font, label(), width - 208, 116, OreChrome.MUTED, false);
        for (var child : children()) if (child instanceof EditBox field) OreChrome.well(graphics, field.getX(), field.getY(), field.getWidth(), field.getHeight());
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String label() {
        return switch (kind) {
            case "place" -> "地点 id。坐标用法杖在世界里写。";
            case "action" -> "行动 id / 类型";
            case "time" -> "时间 id / 行动 / 地点。从节点拖到目标。";
            case "dialog" -> "对话 id。从按钮圆点拖到另一对话。";
            case "route" -> "路线 id / 模式 / 速度。拖到地点追加。";
            default -> "拖动节点。时间连行动或地点，路线拖到地点。";
        };
    }

    private void drawNodes(GuiGraphics graphics) {
        for (int i = 0; i < doc.places().size(); i++) node(graphics, "place", i, doc.places().get(i).id(), OreChrome.GREEN);
        for (int i = 0; i < doc.actions().size(); i++) node(graphics, "action", i, doc.actions().get(i).id(), OreChrome.PURPLE);
        for (int i = 0; i < doc.timeline().size(); i++) node(graphics, "time", i, doc.timeline().get(i).id(), OreChrome.GOLD);
        for (int i = 0; i < doc.dialogs().size(); i++) node(graphics, "dialog", i, doc.dialogs().get(i).id(), OreChrome.SOFT);
        for (int i = 0; i < doc.routes().size(); i++) {
            NpcRoute route = doc.routes().get(i);
            node(graphics, "route", i, route.id() + (route.id().equals(doc.activeRouteId()) ? "*" : ""), 0xFF5AA7E0);
        }
    }

    private void node(GuiGraphics graphics, String nodeKind, int index, String id, int accent) {
        float[] pos = position(nodeKind, index);
        int x = 16 + (int) pos[0];
        int y = 40 + (int) pos[1];
        boolean on = nodeKind.equals(kind) && index == selected;
        graphics.fill(x, y, x + 132, y + 28, on ? OreChrome.SURFACE : OreChrome.DEEP);
        graphics.fill(x, y, x + 132, y + 2, accent);
        graphics.drawString(font, clip(id), x + 6, y + 10, OreChrome.INK, false);
        graphics.fill(x + 122, y + 10, x + 128, y + 18, OreChrome.GOLD);
    }

    private void drawLinks(GuiGraphics graphics) {
        for (int i = 0; i < doc.timeline().size(); i++) {
            TimelineNode node = doc.timeline().get(i);
            int[] from = port("time", i);
            int action = indexOf(doc.actions(), node.actionId(), ActionSpec::id);
            if (action >= 0) line(graphics, from[0], from[1], port("action", action)[0], port("action", action)[1], OreChrome.PURPLE);
            int place = indexOf(doc.places(), node.placeId(), NpcPlace::id);
            if (place >= 0) line(graphics, from[0], from[1], port("place", place)[0], port("place", place)[1], OreChrome.GREEN);
        }
        for (int i = 0; i < doc.dialogs().size(); i++) {
            DialogScript dialog = doc.dialogs().get(i);
            for (int b = 0; b < dialog.buttons().size(); b++) {
                int target = indexOf(doc.dialogs(), dialog.buttons().get(b).dialogId(), DialogScript::id);
                if (target < 0) continue;
                int[] from = port("dialog", i);
                int[] to = port("dialog", target);
                line(graphics, from[0], from[1] + 6, to[0] - 120, to[1], OreChrome.GOLD);
            }
        }
        for (int i = 0; i < doc.routes().size(); i++) {
            int[] from = port("route", i);
            for (NpcRoute.Waypoint point : doc.routes().get(i).points()) {
                int place = indexOf(doc.places(), point.placeId(), NpcPlace::id);
                if (place >= 0) line(graphics, from[0], from[1], port("place", place)[0], port("place", place)[1], OreChrome.GOLD);
            }
        }
    }

    private int[] port(String nodeKind, int index) {
        float[] pos = position(nodeKind, index);
        return new int[] { 16 + (int) pos[0] + 125, 40 + (int) pos[1] + 14 };
    }

    private float[] position(String nodeKind, int index) {
        int column = switch (nodeKind) { case "place" -> 0; case "action" -> 1; case "time" -> 2; case "dialog" -> 3; default -> 4; };
        return at.computeIfAbsent(nodeKind + ":" + index, key -> new float[] { column * 150f, index * 36f });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && clickNode(mouseX, mouseY)) {
            setFocused(null);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickNode(double mouseX, double mouseY) {
        if (hit("route", doc.routes().size(), mouseX, mouseY)) return true;
        if (hit("dialog", doc.dialogs().size(), mouseX, mouseY)) return true;
        if (hit("time", doc.timeline().size(), mouseX, mouseY)) return true;
        if (hit("action", doc.actions().size(), mouseX, mouseY)) return true;
        return hit("place", doc.places().size(), mouseX, mouseY);
    }

    private boolean hit(String nodeKind, int count, double mouseX, double mouseY) {
        for (int i = count - 1; i >= 0; i--) {
            float[] pos = position(nodeKind, i);
            int x = 16 + (int) pos[0];
            int y = 40 + (int) pos[1];
            if (mouseX < x || mouseY < y || mouseX > x + 132 || mouseY > y + 28) continue;
            commitFields();
            kind = nodeKind;
            selected = i;
            fill();
            dragIndex = i;
            originX = mouseX;
            originY = mouseY;
            originNodeX = pos[0];
            originNodeY = pos[1];
            drag = mouseX >= x + 118 ? 2 : 1;
            dragButton = "dialog".equals(nodeKind) ? 0 : -1;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (drag == 1) {
            float[] pos = position(kind, dragIndex);
            pos[0] = Math.max(0, originNodeX + (float) (mouseX - originX));
            pos[1] = Math.max(0, originNodeY + (float) (mouseY - originY));
            return true;
        }
        return drag == 2 || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (drag == 2) link(mouseX, mouseY);
        drag = 0;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void link(double mouseX, double mouseY) {
        String targetKind = "";
        int target = -1;
        for (String candidate : List.of("action", "place", "dialog")) {
            int count = switch (candidate) { case "action" -> doc.actions().size(); case "place" -> doc.places().size(); default -> doc.dialogs().size(); };
            for (int i = 0; i < count; i++) {
                int[] port = port(candidate, i);
                if (Math.abs(mouseX - port[0]) < 70 && Math.abs(mouseY - port[1]) < 18) {
                    targetKind = candidate;
                    target = i;
                }
            }
        }
        if (target < 0) return;
        if ("time".equals(kind) && "action".equals(targetKind)) setTime(dragIndex, doc.actions().get(target).id(), null);
        else if ("time".equals(kind) && "place".equals(targetKind)) setTime(dragIndex, null, doc.places().get(target).id());
        else if ("dialog".equals(kind) && "dialog".equals(targetKind) && target != dragIndex) setDialogLink(dragIndex, doc.dialogs().get(target).id());
        else if ("route".equals(kind) && "place".equals(targetKind)) appendPlace(dragIndex, doc.places().get(target).id());
    }

    private void appendPlace(int index, String placeId) {
        if (index < 0 || index >= doc.routes().size()) return;
        List<NpcRoute> routes = new ArrayList<>(doc.routes());
        NpcRoute next = routes.get(index).appendPlace(placeId);
        if (next == routes.get(index)) return;
        routes.set(index, next);
        doc = doc.withRoutes(routes);
        if (doc.activeRouteId().isBlank()) doc = doc.withActiveRoute(next.id());
        error = "已追加路点";
    }

    private void setTime(int index, String actionId, String placeId) {
        if (index < 0 || index >= doc.timeline().size()) return;
        List<TimelineNode> nodes = new ArrayList<>(doc.timeline());
        TimelineNode old = nodes.get(index);
        nodes.set(index, new TimelineNode(old.id(), old.startMinute(), old.durationMinutes(),
                actionId == null ? old.actionId() : actionId, placeId == null ? old.placeId() : placeId, old.priority()));
        doc = copy(doc.places(), doc.actions(), nodes, doc.dialogs());
        fill();
    }

    private void setDialogLink(int index, String dialogId) {
        if (index < 0 || index >= doc.dialogs().size()) return;
        List<DialogScript> dialogs = new ArrayList<>(doc.dialogs());
        DialogScript script = dialogs.get(index);
        if (script.buttons().isEmpty()) return;
        List<DialogScript.DialogButton> buttons = new ArrayList<>(script.buttons());
        int button = Math.max(0, dragButton);
        if (button >= buttons.size()) button = 0;
        DialogScript.DialogButton old = buttons.get(button);
        buttons.set(button, new DialogScript.DialogButton(old.label(), dialogId, old.actionId()));
        dialogs.set(index, new DialogScript(script.id(), script.mode(), script.lines(), buttons, script.background(), script.prompt(), script.fallback(), script.typeSpeed()));
        doc = copy(doc.places(), doc.actions(), doc.timeline(), dialogs);
    }

    private NpcDocument copy(List<NpcPlace> places, List<ActionSpec> actions, List<TimelineNode> timeline, List<DialogScript> dialogs) {
        return new NpcDocument(doc.id(), doc.displayName(), doc.texture(), doc.homePlaceId(), doc.activeRouteId(), doc.defaultDialogId(),
                doc.anchored(), places, doc.routes(), actions, timeline, dialogs, doc.marginals(), doc.trades(), doc.ai(), doc.loadout());
    }

    private static <T> T at(List<T> list, int index) { return index >= 0 && index < list.size() ? list.get(index) : null; }
    private static <T> int indexOf(List<T> list, String id, java.util.function.Function<T, String> key) {
        if (id == null || id.isBlank()) return -1;
        for (int i = 0; i < list.size(); i++) if (id.equals(key.apply(list.get(i)))) return i;
        return -1;
    }
    private static String clip(String value) { return value != null && value.length() > 16 ? value.substring(0, 15) + "…" : value == null ? "" : value; }
    private static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(1, Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
        for (int i = 0; i <= steps; i += 3) graphics.fill(x0 + (x1 - x0) * i / steps, y0 + (y1 - y0) * i / steps, x0 + (x1 - x0) * i / steps + 2, y0 + (y1 - y0) * i / steps + 2, color);
    }

    @Override public boolean isPauseScreen() { return false; }
}
