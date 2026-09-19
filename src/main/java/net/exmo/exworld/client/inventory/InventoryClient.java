package net.exmo.exworld.client.inventory;

import net.exmo.exworld.client.battle.BattleClient;
import net.exmo.exworld.inventory.InventoryRegistries;
import net.exmo.exworld.network.InventoryPayloads;
import net.exmo.exworld.inventory.ItemFootprint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class InventoryClient {
    private InventoryClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(InventoryClient::menus);
        NeoForge.EVENT_BUS.addListener(InventoryClient::opening);
    }

    public static void openRuleEditor(java.util.Map<String, String> tokens) {
        java.util.Map<String, ItemFootprint> rules = new java.util.LinkedHashMap<>();
        if (tokens != null) tokens.forEach((id, size) -> rules.put(id, ItemFootprint.parse(size)));
        net.exmo.exworld.inventory.FootprintRules clientRules = net.exmo.exworld.inventory.FootprintRules.defaults();
        clientRules.replaceAll(rules);
        net.exmo.exworld.inventory.FootprintRuleStore.installClient(clientRules);
        Minecraft.getInstance().setScreen(new InventoryRuleEditorScreen(rules));
    }

    private static void menus(RegisterMenuScreensEvent event) {
        event.register(InventoryRegistries.PLAYER_BACKPACK.get(), PlayerBackpackScreen::new);
    }

    private static void opening(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.isCreative()) return;
        if (!(event.getNewScreen() instanceof InventoryScreen)) return;
        if (event.getNewScreen() instanceof CreativeModeInventoryScreen) return;
        event.setCanceled(true);
        if (BattleClient.active()) return;
        PacketDistributor.sendToServer(new InventoryPayloads.OpenBackpackPayload());
    }
}
