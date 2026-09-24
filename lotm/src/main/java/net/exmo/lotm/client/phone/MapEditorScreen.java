package net.exmo.lotm.client.phone;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.exmo.lotm.phone.PhonePayloads;

/** Paper-style form for a selected map box. */
public final class MapEditorScreen extends Screen {
    private static final int PAPER = 0xFFF7F4EC;
    private static final int INK = 0xFF1C1C1E;
    private static final int MUTED = 0xFF8E8E93;
    private final JsonObject body;
    private EditBox name;
    private EditBox tag;
    private EditBox info;
    private String editing = "";
    private String kind = "place";
    private int scroll;

    public MapEditorScreen(String json) {
        super(Component.literal("地图"));
        JsonObject parsed;
        try {
            parsed = JsonParser.parseString(json == null ? "{}" : json).getAsJsonObject();
        } catch (RuntimeException ignored) {
            parsed = new JsonObject();
        }
        body = parsed;
        kind = text("kind").isBlank() ? "place" : text("kind");
    }

    public static void open(String json) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new MapEditorScreen(json));
    }

    @Override
    protected void init() {
        int x = width / 2 - 150;
        int y = height / 2 - 110;
        name = box(x + 150, y + 36, 140, "名称，如东街商店");
        tag = box(x + 150, y + 62, 140, "商店 / 加油站");
        info = box(x + 150, y + 88, 140, "人工填写的说明");
        addRenderableWidget(Button.builder(Component.literal("地点"), button -> kind = "place").bounds(x + 150, y + 112, 68, 18).build());
        addRenderableWidget(Button.builder(Component.literal("道路"), button -> kind = "road").bounds(x + 222, y + 112, 68, 18).build());
        addRenderableWidget(Button.builder(Component.literal("放入地图"), button -> save(true)).bounds(x + 150, y + 168, 68, 18).build());
        addRenderableWidget(Button.builder(Component.literal("只改文字"), button -> save(false)).bounds(x + 222, y + 168, 68, 18).build());
        addRenderableWidget(Button.builder(Component.literal("移除"), button -> send("map_delete", true)).bounds(x + 150, y + 190, 68, 18).build());
        addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose()).bounds(x + 222, y + 190, 68, 18).build());
    }

    private EditBox box(int x, int y, int w, String hint) {
        EditBox field = new EditBox(font, x, y, w, 16, Component.literal(hint));
        field.setHint(Component.literal(hint));
        field.setMaxLength(120);
        field.setBordered(false);
        field.setTextColor(INK);
        addRenderableWidget(field);
        return field;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int x = width / 2 - 160;
        int y = height / 2 - 120;
        graphics.fill(x, y, x + 320, y + 240, PAPER);
        graphics.fill(x, y, x + 320, y + 2, 0xFFD7C4A3);
        graphics.drawString(font, "地图编辑", x + 12, y + 10, INK, false);
        graphics.drawString(font, bounds(), x + 150, y + 28, MUTED, false);
        graphics.drawString(font, "当前：" + ("road".equals(kind) ? "道路" : "地点"), x + 150, y + 136, MUTED, false);
        int row = y + 36;
        var places = body.has("places") && body.get("places").isJsonArray() ? body.getAsJsonArray("places") : null;
        if (places != null) {
            int shown = 0;
            for (int i = scroll; i < places.size() && shown < 8; i++) {
                JsonObject place = places.get(i).getAsJsonObject();
                boolean active = text(place, "id").equals(editing);
                if (active) graphics.fill(x + 8, row - 2, x + 138, row + 12, 0xFFE7EFE4);
                graphics.drawString(font, clip(text(place, "name"), 14), x + 10, row, INK, false);
                row += 16;
                shown++;
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = width / 2 - 160;
        int y = height / 2 - 120;
        if (mouseX >= x + 8 && mouseX <= x + 138) {
            int index = scroll + (int) ((mouseY - (y + 36)) / 16);
            var places = body.has("places") && body.get("places").isJsonArray() ? body.getAsJsonArray("places") : null;
            if (places != null && index >= 0 && index < places.size() && mouseY >= y + 34) {
                JsonObject place = places.get(index).getAsJsonObject();
                editing = text(place, "id");
                name.setValue(text(place, "name"));
                tag.setValue(text(place, "tag"));
                info.setValue(text(place, "info"));
                kind = text(place, "kind").isBlank() ? "place" : text(place, "kind");
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
        return true;
    }

    private void save(boolean useBox) {
        send(useBox ? "map_save" : "map_text", useBox);
    }

    private void send(String action, boolean useBox) {
        String json = "{\"action\":\"" + action + "\",\"id\":" + quote(editing) + ",\"name\":" + quote(name.getValue())
                + ",\"tag\":" + quote(tag.getValue()) + ",\"info\":" + quote(info.getValue()) + ",\"kind\":" + quote(kind)
                + ",\"useBox\":" + useBox + ",\"x1\":" + number("x1") + ",\"y1\":" + number("y1") + ",\"z1\":" + number("z1")
                + ",\"x2\":" + number("x2") + ",\"y2\":" + number("y2") + ",\"z2\":" + number("z2") + ",\"dim\":" + quote(text("dim")) + "}";
        PacketDistributor.sendToServer(new PhonePayloads.PhoneActionPayload(json));
    }

    private String bounds() {
        if (!body.has("ready") || !body.get("ready").getAsBoolean()) return "未框选。先点两个方块角。";
        return number("x1") + "," + number("z1") + " → " + number("x2") + "," + number("z2");
    }

    private String text(String key) {
        return text(body, key);
    }

    private static String text(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private int number(String key) {
        try {
            return body.has(key) ? body.get(key).getAsInt() : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String clip(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
