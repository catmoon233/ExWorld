package net.exmo.exworld.client.webview;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.lwjgl.glfw.GLFW;

/** F8 and the client command /exworld webview. The dedicated server never loads this class. */
public final class WebView2Client {
    private static final KeyMapping OPEN = new KeyMapping("key.exworld.open_webview",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "key.categories.exworld");
    private static boolean registered;

    private WebView2Client() {}

    public static void register(IEventBus modBus) {
        if (registered) return;
        registered = true;
        modBus.addListener(WebView2Client::keys);
        NeoForge.EVENT_BUS.addListener(WebView2Client::tick);
        NeoForge.EVENT_BUS.addListener(WebView2Client::commands);
        NeoForge.EVENT_BUS.addListener(WebView2Client::logout);
        NeoForge.EVENT_BUS.addListener(WebView2Client::shutdown);
    }

    private static void keys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (WebView2Window.isOpen() && minecraft.screen == null && minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.releaseMouse();
        }
        while (OPEN.consumeClick()) {
            if (minecraft.screen == null) WebView2Window.open();
        }
    }

    private static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("exworld")
                .then(Commands.literal("webview").executes(context -> {
                    WebView2Window.open();
                    return 1;
                })));
    }

    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        WebView2Window.close();
    }

    private static void shutdown(GameShuttingDownEvent event) {
        WebView2Window.close();
        if (WebView2Platform.supported()) WebView2Host.shutdown();
    }
}
