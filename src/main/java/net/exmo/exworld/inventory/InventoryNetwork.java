package net.exmo.exworld.inventory;

import net.exmo.exworld.network.InventoryPayloads;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.exmo.exmodifier.api.ExModifierApi;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InventoryNetwork {
    private InventoryNetwork() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(InventoryPayloads.OpenBackpackPayload.TYPE, InventoryPayloads.OpenBackpackPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) PlayerBackpack.open(player);
                });
        registrar.playToServer(InventoryPayloads.RotateBackpackPayload.TYPE, InventoryPayloads.RotateBackpackPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof PlayerBackpackMenu menu) {
                        menu.rotate(payload.slot());
                    }
                });
        registrar.playToClient(InventoryPayloads.FootprintRulesPayload.TYPE, InventoryPayloads.FootprintRulesPayload.STREAM_CODEC,
                (payload, context) -> {
                    FootprintRules rules = FootprintRules.defaults();
                    rules.replaceAll(InventoryPayloads.parse(payload.rules()));
                    FootprintRuleStore.installClient(rules);
                });
        registrar.playToServer(InventoryPayloads.CreativeEditPayload.TYPE, InventoryPayloads.CreativeEditPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) return;
                    FootprintRulesSavedData data = FootprintRulesSavedData.get(player.getServer());
                    if (!payload.itemId().isBlank() && !payload.size().isBlank()) {
                        data.set(payload.itemId(), ItemFootprint.parse(payload.size()));
                    }
                    if (!payload.quality().isBlank() && player.containerMenu.getCarried() != null) {
                        ItemStack carried = player.containerMenu.getCarried();
                        if (!carried.isEmpty() && payload.itemId().equals(BuiltInRegistries.ITEM.getKey(carried.getItem()).toString())) {
                            ExModifierApi.setQuality(carried, ResourceLocation.parse(payload.quality()));
                        }
                    }
                    if (payload.save()) {
                        player.getServer().getPlayerList().getPlayers().forEach(InventoryNetwork::sendRules);
                    } else {
                        sendRules(player);
                    }
                });
        registrar.playToServer(InventoryPayloads.RequestFootprintEditorPayload.TYPE, InventoryPayloads.RequestFootprintEditorPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player && player.hasPermissions(2)) {
                        sendEditor(player);
                    }
                });
        registrar.playToServer(InventoryPayloads.FootprintEditPayload.TYPE, InventoryPayloads.FootprintEditPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) return;
                    FootprintRulesSavedData data = FootprintRulesSavedData.get(player.getServer());
                    if (payload.remove()) data.rules().clear(payload.itemId());
                    else if (!payload.itemId().isBlank() && !payload.size().isBlank()) {
                        data.set(payload.itemId(), ItemFootprint.parse(payload.size()));
                    }
                    sendRules(player);
                    sendEditor(player);
                });
        registrar.playToClient(InventoryPayloads.FootprintEditorPayload.TYPE, InventoryPayloads.FootprintEditorPayload.STREAM_CODEC,
                (payload, context) -> net.exmo.exworld.client.inventory.InventoryClient.openRuleEditor(payload.rules()));
    }

    public static void sendEditor(ServerPlayer player) {
        Map<String, String> tokens = new LinkedHashMap<>();
        FootprintRuleStore.server(player.getServer()).snapshot()
                .forEach((id, footprint) -> tokens.put(id, footprint.token()));
        PacketDistributor.sendToPlayer(player, new InventoryPayloads.FootprintEditorPayload(tokens));
    }

    public static void sendRules(ServerPlayer player) {
        sendRules(player, FootprintRuleStore.server(player.getServer()));
    }

    public static void sendRules(ServerPlayer player, FootprintRules rules) {
        Map<String, String> tokens = new LinkedHashMap<>();
        rules.snapshot().forEach((id, footprint) -> tokens.put(id, footprint.token()));
        PacketDistributor.sendToPlayer(player, new InventoryPayloads.FootprintRulesPayload(tokens));
    }
}
