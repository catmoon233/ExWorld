package net.exmo.exworld.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.exmo.exworld.client.screen.WorldMapScreen;
import net.exmo.exworld.client.screen.AnchorMapScreen;
import net.exmo.exworld.client.screen.WorldGroupEditorScreen;
import net.exmo.exworld.client.perspective.DungeonPerspective;
import net.exmo.exworld.client.perspective.FirstPersonToggle;
import net.exmo.exworld.client.ship.ShipClient;
import net.exmo.exworld.client.tooltip.ExWorldTooltips;
import net.exmo.exworld.network.RequestWorldMapPayload;
import net.exmo.exworld.network.RequestRespawnAnchorsPayload;
import net.exmo.exworld.network.WorldGroupEditorPayload;
import net.exmo.exworld.world.model.WorldSnapshot;
import net.exmo.exworld.world.model.AnchorSnapshot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.exmo.exworld.subtitle.client.SubtitleHud;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.client.gui.screens.DeathScreen;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import net.minecraft.resources.ResourceLocation;
import net.exmo.exworld.client.battle.BattleClient;
import net.exmo.exworld.client.battle.BattleHud;
import net.exmo.exworld.client.battle.BattleWorldRenderer;
import net.exmo.exworld.client.camera.AdvancedCameraDirector;
import net.exmo.exworld.network.CardCollectionActionPayload;
import net.exmo.exworld.client.party.PartyHud;
import net.exmo.exworld.client.voice.VoiceCastAddonCompat;
import net.exmo.exworld.client.equipment.EquipmentClient;
import net.exmo.exworld.client.inventory.InventoryClient;
import net.exmo.exworld.client.quest.QuestClient;
import net.exmo.exworld.client.quest.QuestHud;
import net.exmo.exworld.client.quest.QuestNavigationRenderer;
import net.neoforged.neoforge.client.event.InputEvent;

public final class WorldMapClient {
    private static final KeyMapping OPEN_MAP = new KeyMapping("key.exworld.open_world_map",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.exworld");
    private static final KeyMapping OPEN_CARDS = new KeyMapping("key.exworld.open_card_collection",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories.exworld");
    private static final KeyMapping TOGGLE_FIRST_PERSON = new KeyMapping("key.exworld.toggle_first_person",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.exworld");
    private static final KeyMapping OPEN_JOURNAL = new KeyMapping("key.exworld.open_quest_journal",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.exworld");
    private static boolean deckKeyHidden;

    private WorldMapClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(WorldMapClient::registerKeys);
        modBus.addListener(SubtitleHud::registerLayer);
        modBus.addListener(BattleHud::registerLayer);
        modBus.addListener(PartyHud::registerLayer);
        modBus.addListener(QuestHud::registerLayer);
        modBus.addListener(QuestNavigationRenderer::registerLayer);
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(ResourceLocation.fromNamespaceAndPath("exworld", "cinematic"),
                (graphics, delta) -> AdvancedCameraDirector.renderOverlay(graphics)));
        BattleClient.register();
        EquipmentClient.register();
        InventoryClient.register(modBus);
        VoiceCastAddonCompat.register(modBus);
        ShipClient.register(modBus);
        ExWorldTooltips.register();
        NeoForge.EVENT_BUS.addListener(WorldMapClient::clientTick);
        NeoForge.EVENT_BUS.addListener(WorldBoundaryRenderer::render);
        NeoForge.EVENT_BUS.addListener(BattleWorldRenderer::render);
        NeoForge.EVENT_BUS.addListener(WorldMapClient::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(DungeonPerspective::computeAngles);
        NeoForge.EVENT_BUS.addListener(DungeonPerspective::cameraDistance);
        NeoForge.EVENT_BUS.addListener(DungeonPerspective::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(WorldMapClient::questHudMouse);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) { event.register(OPEN_MAP); event.register(OPEN_CARDS); event.register(TOGGLE_FIRST_PERSON); event.register(OPEN_JOURNAL); }

    private static void clientTick(ClientTickEvent.Post event) {
        if (deckKeyHidden != net.exmo.exworld.Config.decryptionMode) {
            deckKeyHidden = net.exmo.exworld.Config.decryptionMode;
            applyDeckKey(deckKeyHidden);
        }
        FirstPersonToggle.enforceDecryption();
        DungeonPerspective.tick();
        FirstPersonToggle.tick();
        BattleClient.tick();
        AdvancedCameraDirector.tick();
        SubtitleHud.tick();
        while (OPEN_MAP.consumeClick()) {
            if (Minecraft.getInstance().player != null) PacketDistributor.sendToServer(new RequestWorldMapPayload());
        }
        while (OPEN_CARDS.consumeClick()) {
            if (!net.exmo.exworld.Config.decryptionMode && !BattleClient.active()) PacketDistributor.sendToServer(new CardCollectionActionPayload(
                    CardCollectionActionPayload.Action.REQUEST, 0, null, java.util.List.of(), ""));
        }
        while (TOGGLE_FIRST_PERSON.consumeClick()) FirstPersonToggle.toggle();
        while (OPEN_JOURNAL.consumeClick()) if (!BattleClient.active()) QuestClient.open();
    }

    public static void applyDeckKey(boolean hide) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) return;
        KeyMapping[] current = minecraft.options.keyMappings;
        boolean present = false;
        for (KeyMapping mapping : current) if (mapping == OPEN_CARDS) present = true;
        if (hide && present) {
            minecraft.options.keyMappings = java.util.Arrays.stream(current).filter(mapping -> mapping != OPEN_CARDS).toArray(KeyMapping[]::new);
        } else if (!hide && !present) {
            KeyMapping[] next = java.util.Arrays.copyOf(current, current.length + 1);
            next[current.length] = OPEN_CARDS;
            minecraft.options.keyMappings = next;
        }
    }

    public static void receive(WorldSnapshot snapshot) {
        Minecraft.getInstance().setScreen(new WorldMapScreen(snapshot));
    }

    public static void receiveAnchors(AnchorSnapshot snapshot) {
        Minecraft.getInstance().setScreen(new AnchorMapScreen(snapshot));
    }

    public static void receiveGroupEditor(WorldGroupEditorPayload payload) {
        Minecraft.getInstance().setScreen(new WorldGroupEditorScreen(payload));
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        if (net.exmo.exworld.Config.decryptionMode && event.getNewScreen() instanceof net.exmo.exworld.client.battle.screen.CardCollectionScreen) {
            event.setCanceled(true);
            return;
        }
        if (event.getNewScreen() instanceof DeathScreen && !(event.getCurrentScreen() instanceof AnchorMapScreen)) {
            PacketDistributor.sendToServer(new RequestRespawnAnchorsPayload());
        }
    }
    private static void questHudMouse(InputEvent.MouseButton.Pre event) { Minecraft minecraft=Minecraft.getInstance(); double scale=minecraft.getWindow().getGuiScale(); if (event.getButton()==GLFW.GLFW_MOUSE_BUTTON_LEFT && minecraft.screen==null && QuestHud.click(minecraft.mouseHandler.xpos()/scale,minecraft.mouseHandler.ypos()/scale)) event.setCanceled(true); }
}
