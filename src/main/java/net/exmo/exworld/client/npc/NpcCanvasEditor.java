package net.exmo.exworld.client.npc;

import net.exmo.exworld.npc.data.DialogScript;
import net.exmo.exworld.npc.data.NpcDocument;
import net.exmo.exworld.npc.data.RelationEdge;
import net.exmo.exworld.npc.data.TimelineNode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Drag canvas for the timeline, dialog links and relation targets. Positions are not saved. */
final class NpcCanvasEditor {
    interface Host {
        NpcDocument document();
        List<RelationEdge> relations();
        List<String> catalogIds();
        int selected();
        void select(int index);
        void commitFields();
        void moveTimeline(int index, int startMinute, int durationMinutes);
        void linkDialog(int dialogIndex, int buttonIndex, String targetDialogId);
        void linkRelation(String toId);
        Font font();
    }

    private static final int SNAP = 14;
    private final Host host;
    private final Map<String, float[]> dialogAt = new LinkedHashMap<>();
    private final Map<String, float[]> relationAt = new LinkedHashMap<>();
    private int x;
    private int y;
    private int w;
    private int h;
    private int tab = -1;
    private int mode;
    private int index = -1;
    private int sub = -1;
    private double originX;
    private double originY;
    private float originNodeX;
    private float originNodeY;
    private int originStart;
    private int originDuration;
    private int barX;
    private int barW;
    private boolean active;

    NpcCanvasEditor(Host host) {
        this.host = host;
    }

    boolean busy() { return active; }

    void render(GuiGraphics graphics, int left, int top, int width, int height, int mouseX, int mouseY, int page) {
        this.x = left;
        this.y = top;
        this.w = Math.max(48, width);
        this.h = Math.max(48, height);
        this.tab = page;
        OreChrome.panel(graphics, x, y, w, h, OreChrome.GOLD);
        if (page == 3) renderTimeline(graphics, mouseX, mouseY);
        else if (page == 5) renderDialogs(graphics, mouseX, mouseY);
        else if (page == 6) renderRelations(graphics, mouseX, mouseY);
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || tab != 3 && tab != 5 && tab != 6) return false;
        if (mouseX < x || mouseY < y || mouseX >= x + w || mouseY >= y + h) return false;
        host.commitFields();
        active = false;
        mode = 0;
        if (tab == 3) return clickTimeline(mouseX, mouseY);
        if (tab == 5) return clickDialog(mouseX, mouseY);
        return clickRelation(mouseX, mouseY);
    }

    boolean mouseDragged(double mouseX, double mouseY) {
        if (!active) return false;
        if (mode == 1 || mode == 2) {
            int delta = barW <= 0 ? 0 : (int) Math.round((mouseX - originX) / barW * 1440.0);
            if (mode == 1) host.moveTimeline(index, Math.floorMod(snap(originStart + delta), 1440), originDuration);
            else host.moveTimeline(index, originStart, Math.max(5, Math.min(1440, snap(originDuration + delta))));
            return true;
        }
        if (mode == 3) {
            float[] pos = dialogAt(dialogId(index), index);
            pos[0] = clamp(originNodeX + (float) (mouseX - originX), 4, w - 124);
            pos[1] = clamp(originNodeY + (float) (mouseY - originY), 4, h - 40);
            return true;
        }
        return mode == 4 || mode == 5;
    }

    boolean mouseReleased(double mouseX, double mouseY) {
        if (!active) return false;
        if (mode == 4) {
            int target = dialogAt(mouseX, mouseY, index);
            if (target >= 0) host.linkDialog(index, sub, host.document().dialogs().get(target).id());
        } else if (mode == 5) {
            String target = relationAt(mouseX, mouseY);
            if (target != null) host.linkRelation(target);
        }
        active = false;
        mode = 0;
        return true;
    }

    private boolean clickTimeline(double mouseX, double mouseY) {
        List<TimelineNode> nodes = host.document().timeline();
        layoutBar();
        int lanes = lanes(nodes.size());
        for (int i = nodes.size() - 1; i >= 0; i--) {
            TimelineNode node = nodes.get(i);
            int top = y + 28 + (i % lanes) * 18;
            if (mouseY < top || mouseY > top + 14) continue;
            if (Math.abs(mouseX - minuteX(node.startMinute() + node.durationMinutes())) <= 6) {
                beginBar(i, 2, node, mouseX);
                return true;
            }
            if (covers(node, mouseX)) {
                beginBar(i, 1, node, mouseX);
                return true;
            }
        }
        return true;
    }

    private void beginBar(int nodeIndex, int dragMode, TimelineNode node, double mouseX) {
        host.select(nodeIndex);
        index = nodeIndex;
        mode = dragMode;
        originX = mouseX;
        originStart = node.startMinute();
        originDuration = node.durationMinutes();
        active = true;
    }

    private boolean clickDialog(double mouseX, double mouseY) {
        List<DialogScript> dialogs = host.document().dialogs();
        for (int i = dialogs.size() - 1; i >= 0; i--) {
            Box box = dialogBox(dialogs.get(i), i);
            int port = portAt(box, dialogs.get(i), mouseX, mouseY);
            if (port >= 0) {
                host.select(i);
                index = i;
                sub = port;
                mode = 4;
                originX = mouseX;
                originY = mouseY;
                active = true;
                return true;
            }
            if (box.contains(mouseX, mouseY)) {
                host.select(i);
                index = i;
                mode = 3;
                originX = mouseX;
                originY = mouseY;
                float[] pos = dialogAt(dialogs.get(i).id(), i);
                originNodeX = pos[0];
                originNodeY = pos[1];
                active = true;
                return true;
            }
        }
        return true;
    }

    private boolean clickRelation(double mouseX, double mouseY) {
        Box self = selfBox();
        if (mouseX >= self.x + self.w - 10 && mouseX <= self.x + self.w && mouseY >= self.y && mouseY <= self.y + self.h) {
            if (host.selected() < 0 && !host.relations().isEmpty()) host.select(0);
            mode = 5;
            originX = mouseX;
            originY = mouseY;
            active = true;
            return true;
        }
        return true;
    }

    private void renderTimeline(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = host.font();
        graphics.drawString(font, "一天 1440 分钟", x + 8, y + 8, OreChrome.GOLD, false);
        layoutBar();
        graphics.fill(barX, y + 20, barX + barW, y + 22, OreChrome.EDGE_LIGHT);
        for (int minute = 0; minute <= 1440; minute += 240) {
            int tick = minuteX(minute);
            graphics.fill(tick, y + 18, tick + 1, y + 24, OreChrome.MUTED);
            if (minute < 1440) graphics.drawString(font, Integer.toString(minute / 60), tick + 2, y + 8, OreChrome.MUTED, false);
        }
        List<TimelineNode> nodes = host.document().timeline();
        int lanes = lanes(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            TimelineNode node = nodes.get(i);
            int top = y + 28 + (i % lanes) * 18;
            int color = i == host.selected() ? OreChrome.GOLD : OreChrome.GREEN;
            drawSpan(graphics, node, top, color);
            graphics.drawString(font, clip(node.id(), 12), minuteX(node.startMinute()) + 4, top + 3, OreChrome.INK_DARK, false);
        }
        graphics.drawString(font, "拖动吸附 5 分钟，右缘改持续", x + 8, y + h - 14, OreChrome.MUTED, false);
    }

    private void renderDialogs(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = host.font();
        graphics.drawString(font, "对话节点", x + 8, y + 8, OreChrome.GOLD, false);
        List<DialogScript> dialogs = host.document().dialogs();
        for (int i = 0; i < dialogs.size(); i++) {
            DialogScript dialog = dialogs.get(i);
            Box box = dialogBox(dialog, i);
            for (int b = 0; b < dialog.buttons().size(); b++) {
                int target = indexOf(dialogs, dialog.buttons().get(b).dialogId());
                if (target < 0) continue;
                Box other = dialogBox(dialogs.get(target), target);
                line(graphics, box.x + box.w - 4, box.y + 22 + b * 14, other.x, other.y + 10, OreChrome.GREEN);
            }
        }
        for (int i = 0; i < dialogs.size(); i++) {
            DialogScript dialog = dialogs.get(i);
            Box box = dialogBox(dialog, i);
            graphics.fill(box.x, box.y, box.x + box.w, box.y + box.h, i == host.selected() ? OreChrome.SURFACE : OreChrome.DEEP);
            graphics.fill(box.x, box.y, box.x + box.w, box.y + 2, OreChrome.GOLD);
            graphics.drawString(font, clip(dialog.id(), 14), box.x + 4, box.y + 4, OreChrome.GOLD, false);
            for (int b = 0; b < dialog.buttons().size(); b++) {
                int row = box.y + 18 + b * 14;
                graphics.drawString(font, clip(dialog.buttons().get(b).label(), 12), box.x + 4, row, OreChrome.INK, false);
                graphics.fill(box.x + box.w - 10, row, box.x + box.w - 2, row + 8, OreChrome.GOLD);
            }
        }
        if (active && mode == 4) line(graphics, (int) originX, (int) originY, mouseX, mouseY, OreChrome.GOLD);
        graphics.drawString(font, "拖按钮圆点吸附到另一节点", x + 8, y + h - 14, OreChrome.MUTED, false);
    }

    private void renderRelations(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = host.font();
        graphics.drawString(font, "关系", x + 8, y + 8, OreChrome.GOLD, false);
        Box self = selfBox();
        List<String> ids = targets();
        List<RelationEdge> edges = host.relations();
        for (int i = 0; i < edges.size(); i++) {
            int target = ids.indexOf(edges.get(i).toId());
            if (target < 0) continue;
            Box other = relationBox(ids.get(target), target);
            line(graphics, self.x + self.w, self.y + self.h / 2, other.x, other.y + other.h / 2, i == host.selected() ? OreChrome.GOLD : OreChrome.MUTED);
        }
        graphics.fill(self.x, self.y, self.x + self.w, self.y + self.h, OreChrome.DEEP);
        graphics.drawString(font, "自身", self.x + 8, self.y + 10, OreChrome.GOLD, false);
        graphics.fill(self.x + self.w - 10, self.y + self.h / 2 - 4, self.x + self.w - 2, self.y + self.h / 2 + 4, OreChrome.GOLD);
        for (int i = 0; i < ids.size(); i++) {
            Box box = relationBox(ids.get(i), i);
            boolean chosen = host.selected() >= 0 && host.selected() < edges.size() && ids.get(i).equals(edges.get(host.selected()).toId());
            graphics.fill(box.x, box.y, box.x + box.w, box.y + box.h, chosen ? OreChrome.GREEN : OreChrome.DEEP);
            graphics.drawString(font, clip(ids.get(i), 16), box.x + 4, box.y + 6, OreChrome.INK, false);
        }
        if (active && mode == 5) line(graphics, (int) originX, (int) originY, mouseX, mouseY, OreChrome.GOLD);
        graphics.drawString(font, "从自身圆点拖到目录 id", x + 8, y + h - 14, OreChrome.MUTED, false);
    }

    private void layoutBar() {
        barX = x + 8;
        barW = Math.max(20, w - 16);
    }

    private int lanes(int count) {
        int room = Math.max(1, (h - 48) / 18);
        return Math.max(1, Math.min(Math.max(count, 1), room));
    }

    private int minuteX(int minute) {
        int wrapped = Math.floorMod(minute, 1440);
        return barX + (int) Math.round(wrapped / 1440.0 * barW);
    }

    private boolean covers(TimelineNode node, double mouseX) {
        int start = minuteX(node.startMinute());
        int end = minuteX(node.startMinute() + node.durationMinutes());
        if (node.startMinute() + node.durationMinutes() <= 1440) return mouseX >= start && mouseX <= start + Math.max(6, end - start);
        return mouseX >= start && mouseX <= barX + barW || mouseX >= barX && mouseX <= end;
    }

    private void drawSpan(GuiGraphics graphics, TimelineNode node, int top, int color) {
        int start = minuteX(node.startMinute());
        int end = minuteX(node.startMinute() + node.durationMinutes());
        if (node.startMinute() + node.durationMinutes() <= 1440) {
            int right = start + Math.max(6, end - start);
            graphics.fill(start, top, right, top + 14, color);
            graphics.fill(right - 4, top, right, top + 14, 0xFF101418);
            return;
        }
        graphics.fill(start, top, barX + barW, top + 14, color);
        graphics.fill(barX, top, Math.max(barX + 6, end), top + 14, color);
        graphics.fill(Math.max(barX, end - 4), top, Math.max(barX + 6, end), top + 14, 0xFF101418);
    }

    private Box dialogBox(DialogScript dialog, int nodeIndex) {
        float[] pos = dialogAt(dialog.id(), nodeIndex);
        int height = Math.max(28, 20 + dialog.buttons().size() * 14);
        return new Box(x + (int) pos[0], y + (int) pos[1], 120, height);
    }

    private float[] dialogAt(String id, int nodeIndex) {
        return dialogAt.computeIfAbsent(id, key -> new float[] { 8 + (nodeIndex % 3) * 132f, 22 + (nodeIndex / 3) * 78f });
    }

    private int portAt(Box box, DialogScript dialog, double mouseX, double mouseY) {
        for (int i = 0; i < dialog.buttons().size(); i++) {
            int row = box.y + 18 + i * 14;
            if (mouseX >= box.x + box.w - 12 && mouseX <= box.x + box.w && mouseY >= row - 2 && mouseY <= row + 10) return i;
        }
        return -1;
    }

    private int dialogAt(double mouseX, double mouseY, int skip) {
        List<DialogScript> dialogs = host.document().dialogs();
        for (int i = 0; i < dialogs.size(); i++) {
            if (i == skip) continue;
            if (dialogBox(dialogs.get(i), i).grow(SNAP).contains(mouseX, mouseY)) return i;
        }
        return -1;
    }

    private String dialogId(int nodeIndex) {
        List<DialogScript> dialogs = host.document().dialogs();
        return nodeIndex >= 0 && nodeIndex < dialogs.size() ? dialogs.get(nodeIndex).id() : "";
    }

    private Box selfBox() {
        return new Box(x + 8, y + Math.max(28, h / 2 - 16), 72, 28);
    }

    private List<String> targets() {
        List<String> ids = new ArrayList<>();
        String self = host.document().id();
        for (String id : host.catalogIds()) if (id != null && !id.isBlank() && !id.equals(self) && !ids.contains(id)) ids.add(id);
        return ids;
    }

    private Box relationBox(String id, int nodeIndex) {
        float[] pos = relationAt.computeIfAbsent(id, key -> new float[] { 110 + (nodeIndex % 2) * 130f, 28 + (nodeIndex / 2) * 36f });
        return new Box(x + (int) pos[0], y + (int) pos[1], 120, 22);
    }

    private String relationAt(double mouseX, double mouseY) {
        List<String> ids = targets();
        for (int i = 0; i < ids.size(); i++) {
            if (relationBox(ids.get(i), i).grow(SNAP).contains(mouseX, mouseY)) return ids.get(i);
        }
        return null;
    }

    private static int indexOf(List<DialogScript> dialogs, String id) {
        if (id == null || id.isBlank()) return -1;
        for (int i = 0; i < dialogs.size(); i++) if (id.equals(dialogs.get(i).id())) return i;
        return -1;
    }

    private static int snap(int minute) {
        return Math.round(minute / 5f) * 5;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String clip(String value, int limit) {
        if (value == null) return "";
        return value.length() > limit ? value.substring(0, limit - 1) + "…" : value;
    }

    private static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps == 0) {
            graphics.fill(x0, y0, x0 + 2, y0 + 2, color);
            return;
        }
        for (int i = 0; i <= steps; i += 2) {
            int px = x0 + (x1 - x0) * i / steps;
            int py = y0 + (y1 - y0) * i / steps;
            graphics.fill(px, py, px + 2, py + 2, color);
        }
    }

    private record Box(int x, int y, int w, int h) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
        }

        Box grow(int pad) {
            return new Box(x - pad, y - pad, w + pad * 2, h + pad * 2);
        }
    }
}
