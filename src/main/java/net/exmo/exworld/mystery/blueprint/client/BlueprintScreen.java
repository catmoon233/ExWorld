package net.exmo.exworld.mystery.blueprint.client;

import com.google.gson.JsonObject;
import net.exmo.exworld.mystery.MysteryPayloads;
import net.exmo.exworld.mystery.blueprint.BlueprintGraph;
import net.exmo.exworld.mystery.client.MysteryClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** One document, graph canvas and typed inspector; published data is never edited in place. */
public final class BlueprintScreen extends Screen {
    private final String graphId;
    private final List<BlueprintGraph.Node> nodes = new ArrayList<>();
    private final List<BlueprintGraph.Edge> edges = new ArrayList<>();
    private final Deque<String> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private int selected = -1;
    private int dragging = -1;
    private int connectFrom = -1;
    private String connectPort = "next";
    private String message = "";
    private EditBox search, kind, value, number;

    public BlueprintScreen(CompoundTag tag) {
        super(Component.literal("剧情蓝图"));
        graphId = tag.getString("id");
        try {
            BlueprintGraph graph = BlueprintGraph.parseDraft(tag.getString("json"));
            nodes.addAll(graph.nodes()); edges.addAll(graph.edges());
        } catch (RuntimeException error) { message = error.getMessage(); }
    }

    public void result(String result) { message = result == null ? "" : result; }

    @Override protected void init() {
        int sx = width - 214;
        button(sx, 6, 66, "存草稿", () -> save("save_draft"));
        button(sx + 70, 6, 66, "发布", () -> save("publish"));
        button(sx + 140, 6, 66, "关闭", this::onClose);
        search = field(sx + 5, 45, 194, "搜索节点");
        kind = field(sx + 5, 123, 194, "类型，如 on_phase_enter");
        value = field(sx + 5, 161, 194, "值 / 资源 ID");
        number = field(sx + 5, 199, 194, "数字参数");
        button(sx + 5, 225, 92, "应用参数", this::commit);
        button(sx + 105, 225, 92, "删除节点", this::deleteSelected);
        button(sx + 5, 251, 62, "触发", () -> add("trigger"));
        button(sx + 71, 251, 62, "条件", () -> add("condition"));
        button(sx + 137, 251, 62, "动作", () -> add("action"));
        button(sx + 5, 275, 62, "演出", () -> add("cue"));
        button(sx + 71, 275, 62, "等待", () -> add("wait"));
        button(sx + 137, 275, 62, "断点", this::breakpoint);
        button(sx + 5, 301, 62, "连 next", () -> connect("next"));
        button(sx + 71, 301, 62, "连 true", () -> connect("true"));
        button(sx + 137, 301, 62, "连 false", () -> connect("false"));
        button(sx + 5, 327, 62, "撤销", this::undo);
        button(sx + 71, 327, 62, "重做", this::redo);
        button(sx + 137, 327, 62, "预览", this::preview);
        fillInspector();
    }

    private void button(int x, int y, int w, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), unused -> action.run()).bounds(x, y, w, 20).build());
    }

    private EditBox field(int x, int y, int w, String hint) {
        EditBox box = new EditBox(font, x, y, w, 18, Component.literal(hint));
        box.setMaxLength(256); box.setHint(Component.literal(hint));
        addRenderableWidget(box); return box;
    }

    private String json() { return new BlueprintGraph(net.minecraft.resources.ResourceLocation.parse(graphId), 1, nodes, edges).json(); }

    private void snapshot() {
        undo.push(json()); if (undo.size() > 50) undo.removeLast(); redo.clear();
    }

    private void save(String action) {
        commit();
        try {
            BlueprintGraph graph = action.equals("publish") ? BlueprintGraph.parse(json()) : BlueprintGraph.parseDraft(json());
            CompoundTag tag = new CompoundTag();
            tag.putString("kind", action); tag.putString("id", graphId); tag.putString("json", graph.json());
            PacketDistributor.sendToServer(new MysteryPayloads.Server("editor", tag));
            message = action.equals("publish") ? "正在发布…" : "正在保存…";
        } catch (RuntimeException error) { message = "校验失败：" + error.getMessage(); }
    }

    private void add(String type) {
        snapshot();
        JsonObject params = new JsonObject();
        if (type.equals("cue")) { params.addProperty("id", "clue_found"); params.addProperty("duration", 80); }
        else if (type.equals("wait")) params.addProperty("ticks", 20);
        else params.addProperty("kind", type.equals("trigger") ? "on_phase_enter" :
                type.equals("condition") ? "phase" : "add_clue");
        String id = type + "_" + System.nanoTime();
        nodes.add(new BlueprintGraph.Node(id, type, params, 30 + nodes.size() % 3 * 145,
                40 + nodes.size() / 3 * 48));
        selected = nodes.size() - 1;
        fillInspector();
    }

    private void deleteSelected() {
        if (selected < 0) return;
        snapshot();
        String id = nodes.remove(selected).id();
        edges.removeIf(edge -> edge.from().equals(id) || edge.to().equals(id));
        selected = -1; fillInspector();
    }

    private void connect(String port) {
        if (selected < 0) return;
        if (!port.equals("next") && !nodes.get(selected).type().equals("condition")) {
            message = "只有条件节点可使用 true/false"; return;
        }
        connectFrom = selected; connectPort = port; message = "点击目标节点完成连线";
    }

    private void commit() {
        if (selected < 0) return;
        BlueprintGraph.Node old = nodes.get(selected);
        JsonObject params = old.params().deepCopy();
        if (old.type().equals("cue")) {
            params.addProperty("id", value.getValue().trim());
            try { params.addProperty("duration", Integer.parseInt(number.getValue().trim())); }
            catch (NumberFormatException ignored) { params.addProperty("duration", 80); }
        } else if (old.type().equals("wait")) {
            try { params.addProperty("ticks", Integer.parseInt(number.getValue().trim())); }
            catch (NumberFormatException ignored) { params.addProperty("ticks", 20); }
        } else {
            params.addProperty("kind", kind.getValue().trim());
            params.addProperty("value", value.getValue().trim());
            if (old.type().equals("action")) {
                try { params.addProperty("amount", Integer.parseInt(number.getValue().trim())); }
                catch (NumberFormatException ignored) {}
            } else if (old.type().equals("condition")) {
                try { params.addProperty("min", Integer.parseInt(number.getValue().trim())); }
                catch (NumberFormatException ignored) {}
            }
        }
        if (!params.equals(old.params())) {
            snapshot();
            nodes.set(selected, new BlueprintGraph.Node(old.id(), old.type(), params, old.x(), old.y()));
        }
    }

    private void fillInspector() {
        if (kind == null) return;
        kind.setValue(""); value.setValue(""); number.setValue("");
        if (selected < 0 || selected >= nodes.size()) return;
        BlueprintGraph.Node node = nodes.get(selected);
        kind.setValue(node.text("kind"));
        value.setValue(node.type().equals("cue") ? node.text("id") : node.text("value"));
        if (node.type().equals("cue")) number.setValue(Integer.toString(node.number("duration", 80)));
        else if (node.type().equals("wait")) number.setValue(Integer.toString(node.number("ticks", 20)));
        else number.setValue(Integer.toString(node.number("amount", node.number("min", 0))));
    }

    private void breakpoint() {
        if (selected < 0) return;
        snapshot();
        BlueprintGraph.Node old = nodes.get(selected);
        JsonObject params = old.params().deepCopy();
        params.addProperty("breakpoint", !params.has("breakpoint") || !params.get("breakpoint").getAsBoolean());
        nodes.set(selected, new BlueprintGraph.Node(old.id(), old.type(), params, old.x(), old.y()));
    }

    private void preview() {
        commit();
        if (selected >= 0 && nodes.get(selected).type().equals("cue")) {
            BlueprintGraph.Node node = nodes.get(selected);
            MysteryClient.previewCue(node.text("id"), node.number("duration", 80), this);
            message = "单人演出预览：" + node.text("id"); return;
        }
        for (BlueprintGraph.Node node : nodes) {
            if (node.params().has("breakpoint") && node.params().get("breakpoint").getAsBoolean()) {
                message = "预览停在断点：" + node.id(); return;
            }
            if (node.type().equals("cue")) {
                MysteryClient.previewCue(node.text("id"), node.number("duration", 80), this);
                message = "单人演出预览：" + node.text("id"); return;
            }
        }
        message = "没有可预览的演出节点";
    }

    private void undo() { if (undo.isEmpty()) return; redo.push(json()); replace(undo.pop()); }
    private void redo() { if (redo.isEmpty()) return; undo.push(json()); replace(redo.pop()); }
    private void replace(String json) {
        try {
            BlueprintGraph graph = BlueprintGraph.parseDraft(json);
            nodes.clear(); nodes.addAll(graph.nodes()); edges.clear(); edges.addAll(graph.edges());
            selected = -1; fillInspector();
        } catch (RuntimeException error) { message = error.getMessage(); }
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF10151A);
        graphics.fill(8, 30, width - 222, height - 8, 0xFF1E2833);
        graphics.fill(width - 216, 30, width - 8, height - 8, 0xFF2A303D);
        graphics.drawString(font, graphId, 12, 9, 0xFFFFE4AD, false);
        if (!message.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(message, width - 250), 12, height - 21, 0xFFFFAA88, false);
        graphics.drawString(font, "节点搜索", width - 208, 35, 0xFFBCC8D3, false);
        graphics.drawString(font, "类型", width - 208, 111, 0xFFBCC8D3, false);
        graphics.drawString(font, "值 / 演出 ID", width - 208, 149, 0xFFBCC8D3, false);
        graphics.drawString(font, "数量 / 时长", width - 208, 187, 0xFFBCC8D3, false);
        for (BlueprintGraph.Edge edge : edges) {
            BlueprintGraph.Node from = node(edge.from()), to = node(edge.to());
            if (from == null || to == null) continue;
            int x1 = 20 + (int)from.x() + 116, y1 = 40 + (int)from.y() + 13;
            int x2 = 20 + (int)to.x(), y2 = 40 + (int)to.y() + 13;
            int color = edge.port().equals("false") ? 0xFFE77E7E : 0xFFDBB874;
            graphics.hLine(Math.min(x1, x2), Math.max(x1, x2), y1, color);
            graphics.vLine(x2, Math.min(y1, y2), Math.max(y1, y2), color);
        }
        String filter = search == null ? "" : search.getValue().toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < nodes.size(); i++) {
            BlueprintGraph.Node node = nodes.get(i);
            if (!filter.isBlank() && !node.id().toLowerCase(java.util.Locale.ROOT).contains(filter)
                    && !node.type().contains(filter) && !node.text("kind").contains(filter)) continue;
            int x = 20 + (int)node.x(), y = 40 + (int)node.y();
            int color = switch (node.type()) {
                case "trigger" -> 0xFFE4B85A; case "condition" -> 0xFFA999D8;
                case "action" -> 0xFF80C3A0; case "cue" -> 0xFF75B5DD; default -> 0xFFBEC8D0;
            };
            graphics.fill(x, y, x + 122, y + 29, selected == i ? 0xFF475869 : 0xFF30404D);
            graphics.fill(x, y, x + 122, y + 3, color);
            graphics.drawString(font, font.plainSubstrByWidth(node.id(), 108), x + 6, y + 10, 0xFFEAF0F3, false);
            if (node.params().has("breakpoint") && node.params().get("breakpoint").getAsBoolean())
                graphics.fill(x + 113, y + 10, x + 119, y + 16, 0xFFFF6565);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private BlueprintGraph.Node node(String id) {
        for (BlueprintGraph.Node node : nodes) if (node.id().equals(id)) return node;
        return null;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX < width - 222 && mouseY > 30) {
            for (int i = nodes.size() - 1; i >= 0; i--) {
                BlueprintGraph.Node node = nodes.get(i);
                int x = 20 + (int)node.x(), y = 40 + (int)node.y();
                if (mouseX < x || mouseX > x + 122 || mouseY < y || mouseY > y + 29) continue;
                if (connectFrom >= 0 && connectFrom != i) {
                    snapshot();
                    edges.add(new BlueprintGraph.Edge(nodes.get(connectFrom).id(), connectPort, node.id()));
                    connectFrom = -1; message = "已连线";
                } else {
                    commit(); selected = i; snapshot(); dragging = i; fillInspector();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging >= 0 && button == 0 && dragging < nodes.size()) {
            BlueprintGraph.Node old = nodes.get(dragging);
            nodes.set(dragging, new BlueprintGraph.Node(old.id(), old.type(), old.params(),
                    Math.max(0, old.x() + (float)dragX), Math.max(0, old.y() + (float)dragY)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
