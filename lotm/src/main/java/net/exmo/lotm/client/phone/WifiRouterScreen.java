package net.exmo.lotm.client.phone;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.exmo.lotm.phone.PhonePayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Paper form for a placed WiFi router's name, password and range. */
public final class WifiRouterScreen extends Screen {
    private static final int PAPER = 0xFFF7F4EC;
    private static final int INK = 0xFF1C1C1E;
    private final int x;
    private final int y;
    private final int z;
    private final String initialName;
    private final String initialPassword;
    private final int initialRange;
    private EditBox name;
    private EditBox password;
    private EditBox range;

    public WifiRouterScreen(String json) {
        super(Component.literal("WiFi 路由器"));
        JsonObject body = parse(json);
        x = number(body, "x");
        y = number(body, "y");
        z = number(body, "z");
        initialName = text(body, "name");
        initialPassword = text(body, "password");
        initialRange = Math.max(4, number(body, "range"));
    }

    public static void open(String json) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new WifiRouterScreen(json));
    }

    @Override
    protected void init() {
        int left = formLeft() + 16;
        int top = formTop();
        name = field(left, top + 58, initialName);
        password = field(left, top + 96, initialPassword);
        range = field(left, top + 134, Integer.toString(initialRange));
        addRenderableWidget(Button.builder(Component.literal("保存"), button -> save()).bounds(left, top + 162, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose()).bounds(left + 98, top + 162, 90, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = formLeft();
        int top = formTop();
        graphics.fill(left, top, left + 220, top + 208, PAPER);
        graphics.drawString(font, "WiFi 路由器", left + 16, top + 14, INK, false);
        graphics.drawString(font, x + ", " + y + ", " + z, left + 16, top + 28, 0xFF8E8E93, false);
        graphics.drawString(font, "名称", left + 16, top + 46, INK, false);
        graphics.drawString(font, "密码", left + 16, top + 84, INK, false);
        graphics.drawString(font, "范围 4-64", left + 16, top + 122, INK, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int formLeft() {
        return width / 2 - 110;
    }

    private int formTop() {
        return height / 2 - 104;
    }

    private EditBox field(int x, int y, String value) {
        EditBox box = new EditBox(font, x, y, 188, 16, Component.empty());
        box.setMaxLength(32);
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    private void save() {
        int blocks = initialRange;
        try {
            blocks = Integer.parseInt(range.getValue().trim());
        } catch (NumberFormatException ignored) {
        }
        String json = "{\"type\":\"phone\",\"action\":\"wifi_save\",\"x\":" + x + ",\"y\":" + y + ",\"z\":" + z
                + ",\"name\":" + quote(name.getValue()) + ",\"text\":" + quote(password.getValue()) + ",\"range\":" + blocks + "}";
        PacketDistributor.sendToServer(new PhonePayloads.PhoneActionPayload(json));
        onClose();
    }

    private static String quote(String value) {
        return "\"" + (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static JsonObject parse(String json) {
        try {
            return JsonParser.parseString(json == null ? "{}" : json).getAsJsonObject();
        } catch (RuntimeException ignored) {
            return new JsonObject();
        }
    }

    private static String text(JsonObject body, String key) {
        return body.has(key) && !body.get(key).isJsonNull() ? body.get(key).getAsString() : "";
    }

    private static int number(JsonObject body, String key) {
        try {
            return body.has(key) ? body.get(key).getAsInt() : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }
}
