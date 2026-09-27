package net.exmo.exkeys.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.exmo.exkeys.PolicyMode;
import net.exmo.exkeys.mixin.KeyMappingAccess;
import net.exmo.exkeys.mixin.OptionsSubScreenAccess;
import net.exmo.exkeys.network.ExKeysPayloads;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ExKeysClient {
    private static KeyPolicyState state = KeyPolicyState.local(net.exmo.exkeys.KeyPolicy.EMPTY);
    private static boolean wasRemote;

    private ExKeysClient() {}

    public static void register() {
        state = KeyPolicyState.local(KeyCaches.loadLocal());
        NeoForge.EVENT_BUS.addListener(ExKeysClient::screenInit);
        NeoForge.EVENT_BUS.addListener(ExKeysClient::login);
        NeoForge.EVENT_BUS.addListener(ExKeysClient::logout);
        NeoForge.EVENT_BUS.addListener(ExKeysClient::tick);
    }

    public static net.exmo.exkeys.KeyPolicy active() {
        return state.policy();
    }

    public static boolean canEdit() {
        if (mode() == PolicyMode.Kind.LOCAL) return true;
        return operator();
    }


    private static boolean operator() {
        Minecraft minecraft = Minecraft.getInstance();
        return state.editable() || (minecraft.player != null && minecraft.player.hasPermissions(2));
    }

    public static boolean blocks(KeyMapping mapping) {
        return mapping != null && state.policy().blockedFor(mapping.getName(), restrictsAsOperator());
    }

    public static boolean hides(KeyMapping mapping) {
        return mapping != null && state.policy().hiddenFrom(mapping.getName(), restrictsAsOperator());
    }

    private static boolean restrictsAsOperator() {
        if (mode() == PolicyMode.Kind.LOCAL) return true;
        return operator();
    }

    public static PolicyMode.Kind mode() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean inWorld = minecraft.player != null && minecraft.getConnection() != null;
        boolean singleplayer = minecraft.hasSingleplayerServer();
        boolean published = singleplayer && minecraft.getSingleplayerServer() != null && minecraft.getSingleplayerServer().isPublished();
        return PolicyMode.choose(inWorld, singleplayer, published);
    }

    public static void receive(ExKeysPayloads.PolicyPayload payload) {
        if (payload == null || mode() != PolicyMode.Kind.REMOTE) return;
        var parsed = net.exmo.exkeys.KeyPolicy.parse(payload.json(), false);
        if (!parsed.ok()) return;
        state = new KeyPolicyState(parsed.policy(), true, payload.editable());
        KeyCaches.saveRemote(serverKey(), parsed.policy());
        releaseBlocked();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof KeyPolicyScreen screen) {
            screen.onRemote(parsed.policy(), payload.editable());
        } else refreshKeyBinds(minecraft);
    }

    public static void pull() {
        if (mode() != PolicyMode.Kind.REMOTE || Minecraft.getInstance().getConnection() == null) return;
        PacketDistributor.sendToServer(new ExKeysPayloads.PullPayload());
    }

    public static boolean saveLocal(net.exmo.exkeys.KeyPolicy policy) {
        if (!KeyCaches.saveLocal(policy)) return false;
        state = KeyPolicyState.local(policy);
        releaseBlocked();
        return true;
    }

    public static void push(net.exmo.exkeys.KeyPolicy policy) {
        if (mode() != PolicyMode.Kind.REMOTE) {
            saveLocal(policy);
            return;
        }
        if (Minecraft.getInstance().getConnection() == null) return;
        PacketDistributor.sendToServer(new ExKeysPayloads.PushPayload(policy.toJson()));
    }

    public static void releaseBlocked(InputConstants.Key key) {
        Minecraft minecraft = Minecraft.getInstance();
        if (key == null || minecraft.options == null) return;
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (blocks(mapping) && matches(mapping, key)) silence(mapping);
        }
    }

    public static void silence(KeyMapping mapping) {
        if (mapping == null) return;
        mapping.setDown(false);
        ((KeyMappingAccess) mapping).exkeys$setClickCount(0);
    }

    public static String serverKey() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || minecraft.getConnection().getConnection() == null) return "";
        var address = minecraft.getConnection().getConnection().getRemoteAddress();
        return address == null ? "" : address.toString();
    }

    private static void screenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof KeyBindsScreen screen)) return;
        event.addListener(Button.builder(Component.translatable("exkeys.button.configure"), button ->
                        Minecraft.getInstance().setScreen(new KeyPolicyScreen(screen)))
                .bounds(screen.width - 72, 4, 64, 20)
                .build());
    }

    private static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        boolean remote = mode() == PolicyMode.Kind.REMOTE;
        wasRemote = remote;
        if (!remote) {
            state = KeyPolicyState.local(KeyCaches.loadLocal());
            return;
        }
        state = new KeyPolicyState(KeyCaches.loadRemote(serverKey()), false, false);
        pull();
    }

    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        wasRemote = false;
        state = KeyPolicyState.local(KeyCaches.loadLocal());
    }

    private static void tick(ClientTickEvent.Post event) {
        boolean remote = mode() == PolicyMode.Kind.REMOTE;
        if (remote && !wasRemote) {
            state = new KeyPolicyState(KeyCaches.loadRemote(serverKey()), false, false);
            pull();
        } else if (!remote && wasRemote) {
            state = KeyPolicyState.local(KeyCaches.loadLocal());
        }
        wasRemote = remote;
        releaseBlocked();
    }

    private static void releaseBlocked() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) return;
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (blocks(mapping)) silence(mapping);
        }
    }

    private static boolean matches(KeyMapping mapping, InputConstants.Key key) {
        if (key.getType() == InputConstants.Type.MOUSE) return mapping.matchesMouse(key.getValue());
        if (key.getType() == InputConstants.Type.KEYSYM) return mapping.matches(key.getValue(), -1);
        if (key.getType() == InputConstants.Type.SCANCODE) return mapping.matches(-1, key.getValue());
        return false;
    }

    private static void refreshKeyBinds(Minecraft minecraft) {
        if (!(minecraft.screen instanceof KeyBindsScreen screen)) return;
        minecraft.setScreen(new KeyBindsScreen(((OptionsSubScreenAccess) screen).exkeys$lastScreen(), minecraft.options));
    }

    private record KeyPolicyState(net.exmo.exkeys.KeyPolicy policy, boolean received, boolean editable) {
        static KeyPolicyState local(net.exmo.exkeys.KeyPolicy policy) {
            return new KeyPolicyState(policy == null ? net.exmo.exkeys.KeyPolicy.EMPTY : policy, true, true);
        }
    }
}
