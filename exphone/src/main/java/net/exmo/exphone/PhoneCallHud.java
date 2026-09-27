package net.exmo.exphone;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** 独立的通话 HUD：不依赖 SVC 的群组界面，向玩家常驻展示来电 / 呼叫 / 通话状态。 */
public final class PhoneCallHud {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(ExPhone.MODID, "phone_call_hud");
    private static volatile String phase = "idle";
    private static volatile String peer = "";
    private static volatile long startedAt;
    private static volatile String lastStatus = "";
    private static volatile long statusUntil;

    private PhoneCallHud() {}

    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER, (graphics, partial) -> render(graphics));
    }

    /** PhoneClient 把服务端 patch（callState / status 段）转交到这里。 */
    public static void receive(String json) {
        if (json == null) return;
        try {
            JsonObject packet = JsonParser.parseString(json).getAsJsonObject();
            if (!packet.has("section")) return;
            String section = packet.get("section").getAsString();
            if (section.equals("callState") && packet.get("value").isJsonObject()) {
                JsonObject call = packet.getAsJsonObject("value");
                phase = call.has("phase") ? call.get("phase").getAsString() : "idle";
                peer = call.has("peer") ? call.get("peer").getAsString() : "";
                startedAt = call.has("startedAt") ? call.get("startedAt").getAsLong() : 0L;
            } else if (section.equals("status")) {
                lastStatus = packet.get("value").getAsString();
                statusUntil = System.currentTimeMillis() + 6000L;
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!phase.equals("idle")) {
            panel(g);
        } else if (System.currentTimeMillis() < statusUntil && callRelated(lastStatus)) {
            flash(g);
        }
    }

    private static void panel(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int headColor = switch (phase) {
            case "incoming" -> 0xFFF2C14E;
            case "outgoing" -> 0xFF5CB8FF;
            default -> 0xFF5CE27F;
        };
        String head = switch (phase) {
            case "incoming" -> Component.translatable("hud.exphone.phone.incoming", peer).getString();
            case "outgoing" -> Component.translatable("hud.exphone.phone.outgoing", peer).getString();
            default -> Component.translatable("hud.exphone.phone.active", peer).getString();
        };
        boolean live = phase.equals("active") && startedAt > 0;
        String time = live
                ? String.format(java.util.Locale.ROOT, "%02d:%02d",
                        (System.currentTimeMillis() - startedAt) / 60000,
                        (System.currentTimeMillis() - startedAt) / 1000 % 60)
                : "";
        String hint = Component.translatable("hud.exphone.phone.hint_" + phase).getString();
        int lineW = Math.max(font.width(head), font.width(live ? time + "  ·  " + hint : hint));
        int panelW = lineW + 28;
        int x = w / 2 - panelW / 2;
        int y = h / 5;
        int panelH = live ? 56 : 42;
        g.fill(x + 3, y, x + panelW - 3, y + panelH, 0xD9121821);
        g.fill(x, y + 3, x + panelW, y + panelH - 3, 0xD9121821);
        g.fill(x + 3, y, x + panelW - 3, y + 1, headColor);
        g.drawString(font, head, x + (panelW - font.width(head)) / 2, y + 9, headColor, true);
        if (live) {
            String line = time + "  ·  " + hint;
            g.drawString(font, time, x + (panelW - font.width(line)) / 2, y + 25, 0xFFF4F6F8, true);
            g.drawString(font, hint, x + (panelW - font.width(line)) / 2 + font.width(time + "  ·  "), y + 25, 0xFF9AA7B5, true);
        } else {
            g.drawString(font, hint, x + (panelW - font.width(hint)) / 2, y + 25, 0xFF9AA7B5, true);
        }
    }

    private static void flash(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int panelW = font.width(lastStatus) + 24;
        int x = w / 2 - panelW / 2;
        int y = h / 5;
        g.fill(x, y, x + panelW, y + 22, 0xE0141620);
        g.drawString(font, lastStatus, x + 12, y + 7, 0xFFF2C14E, true);
    }

    private static boolean callRelated(String text) {
        if (text == null || text.isBlank()) return false;
        return text.contains("通话") || text.contains("电话") || text.contains("挂断") || text.contains("来电")
                || text.contains("呼叫") || text.contains("接听") || text.contains("没电");
    }
}