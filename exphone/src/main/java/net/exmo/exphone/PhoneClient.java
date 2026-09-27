package net.exmo.exphone;

import net.exmo.exphone.api.PhoneUiHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PhoneClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("exphone");

    private PhoneClient() {}

    public static void open() {
        closeSeparateWindow();
        try {
            PhoneApricity.open();
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.warn("ApricityUI phone screen failed", failure);
            tell("message.exphone.phone.ui_failed");
            return;
        }
        PacketDistributor.sendToServer(new PhonePayloads.PhoneActionPayload("{\"action\":\"state\"}"));
    }

    private static void tell(String key) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(key), false);
        }
    }

    private static void closeSeparateWindow() {
        PhoneUiHooks.opening();
    }

    public static void state(String json) {
        PhoneCallHud.receive(json);
        if (json != null && json.contains("\"openWifi\":true")) {
            net.exmo.exphone.client.WifiRouterScreen.open(json);
            return;
        }
        if (json != null && json.contains("\"openAtm\":true")) {
            net.exmo.exphone.client.AtmScreen.open(json);
            return;
        }
        if (json != null && json.contains("\"openMap\":true")) {
            net.exmo.exphone.client.MapEditorScreen.open(json);
            return;
        }
        PhoneApricity.apply(json);
    }

    static void fromPage(String json) {
        if (json == null || (!json.contains("\"phone\"") && !json.contains("\"action\""))) return;
        Minecraft.getInstance().execute(() -> PacketDistributor.sendToServer(new PhonePayloads.PhoneActionPayload(json)));
    }
}
