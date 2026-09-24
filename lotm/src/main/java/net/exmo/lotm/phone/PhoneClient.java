package net.exmo.lotm.phone;

import net.exmo.exworld.client.webview.WebView2Window;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PhoneClient {
    private PhoneClient() {}

    public static void open() {
        closeSeparateWindow();
        if (!embedded()) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("晴雪 UI 未加载，手机不会弹出独立窗口"), false);
            }
            return;
        }
        PacketDistributor.sendToServer(new PhonePayloads.PhoneActionPayload("{\"action\":\"state\"}"));
    }

    private static boolean embedded() {
        try {
            PhoneApricity.open();
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void closeSeparateWindow() {
        try {
            WebView2Window.close();
        } catch (RuntimeException ignored) {
        }
    }

    public static void state(String json) {
        if (json != null && json.contains("\"openWifi\":true")) {
            net.exmo.lotm.client.phone.WifiRouterScreen.open(json);
            return;
        }
        if (json != null && json.contains("\"openMap\":true")) {
            net.exmo.lotm.client.phone.MapEditorScreen.open(json);
            return;
        }
        PhoneApricity.apply(json);
        if (json != null && json.contains("\"已打开曝光相机\"")) {
            PhoneApricity.close();
        }
    }

    public static void register(net.neoforged.neoforge.network.registration.PayloadRegistrar registrar) {
        registrar.playToClient(PhonePayloads.PhoneStatePayload.TYPE, PhonePayloads.PhoneStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> state(payload.json())));
    }

    static void fromPage(String json) {
        if (json == null || (!json.contains("\"phone\"") && !json.contains("\"action\""))) return;
        Minecraft.getInstance().execute(() -> PacketDistributor.sendToServer(new PhonePayloads.PhoneActionPayload(json)));
    }
}
