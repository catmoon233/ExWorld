package net.exmo.exphone.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.exmo.exphone.PhonePayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Paper form, same sheet as the WiFi router, for withdrawing and depositing banknotes. */
public final class AtmScreen extends Screen {
    private static final int PAPER = 0xFFF7F4EC;
    private static final int INK = 0xFF1C1C1E;
    private static final int[] FACES = {1, 5, 10, 20, 50, 100};
    private long gold;
    private boolean admin;
    private String status = "";
    private int denom = 10;
    private String countText = "1";
    private String creditText = "100";
    private EditBox count;
    private EditBox credit;

    public AtmScreen(String json) {
        super(Component.literal("ATM"));
        apply(json);
    }

    public static void open(String json) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.screen instanceof AtmScreen screen) screen.refresh(json);
        else minecraft.setScreen(new AtmScreen(json));
    }

    public void refresh(String json) {
        remember();
        apply(json);
        clearWidgets();
        init();
    }

    @Override
    protected void init() {
        int left = formLeft() + 16;
        for (int i = 0; i < FACES.length; i++) {
            int face = FACES[i];
            int rowTop = formTop() + (i < 3 ? 78 : 100);
            addRenderableWidget(Button.builder(Component.literal(face == denom ? "[" + face + "]" : Integer.toString(face)),
                    button -> select(face)).bounds(left + (i % 3) * 68, rowTop, 64, 18).build());
        }
        count = field(left, formTop() + 128, countText);
        addRenderableWidget(Button.builder(Component.literal("提出纸币"), button -> send("atm_withdraw")).bounds(left, formTop() + 152, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("存入纸币"), button -> send("atm_deposit")).bounds(left + 104, formTop() + 152, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("全部存入"), button -> send("atm_deposit_all")).bounds(left, formTop() + 176, 200, 20).build());
        if (admin) {
            credit = field(left, formTop() + 224, creditText);
            addRenderableWidget(Button.builder(Component.literal("给自己充钱"), button -> send("atm_credit")).bounds(left, formTop() + 248, 200, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose()).bounds(left, formTop() + heightOfForm() - 36, 200, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = formLeft();
        int top = formTop();
        int height = heightOfForm();
        graphics.fill(left, top, left + 248, top + height, PAPER);
        graphics.drawString(font, "ATM", left + 16, top + 14, INK, false);
        graphics.drawString(font, "余额 " + gold, left + 16, top + 28, 0xFF8E8E93, false);
        if (!status.isEmpty()) graphics.drawString(font, trim(status), left + 16, top + 42, 0xFF8E8E93, false);
        graphics.drawString(font, "面额", left + 16, top + 64, INK, false);
        graphics.drawString(font, "数量", left + 16, top + 116, INK, false);
        if (admin) graphics.drawString(font, "管理员充值", left + 16, top + 210, INK, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int heightOfForm() {
        return admin ? 312 : 248;
    }

    private int formLeft() {
        return width / 2 - 124;
    }

    private int formTop() {
        return Math.max(8, height / 2 - heightOfForm() / 2);
    }

    private void select(int face) {
        remember();
        denom = face;
        clearWidgets();
        init();
    }

    private void remember() {
        if (count != null) countText = count.getValue();
        if (credit != null) creditText = credit.getValue();
    }

    private EditBox field(int x, int y, String value) {
        EditBox box = new EditBox(font, x, y, 200, 16, Component.empty());
        box.setMaxLength(9);
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    private void send(String action) {
        remember();
        int notes = number(countText);
        int money = number(creditText);
        String json = "{\"type\":\"phone\",\"action\":\"" + action + "\",\"denom\":" + denom
                + ",\"count\":" + notes + ",\"amount\":" + money + "}";
        PacketDistributor.sendToServer(new PhonePayloads.PhoneActionPayload(json));
    }

    private void apply(String json) {
        JsonObject body = parse(json);
        gold = number(body, "gold");
        admin = body.has("admin") && body.get("admin").getAsBoolean();
        status = text(body, "status");
    }

    private static String trim(String value) {
        return value.length() <= 28 ? value : value.substring(0, 28);
    }

    private static int number(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException ignored) {
            return 0;
        }
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

    private static long number(JsonObject body, String key) {
        try {
            return body.has(key) ? body.get(key).getAsLong() : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }
}
