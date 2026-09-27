package net.exmo.exworld.mystery.blueprint;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.mystery.Era;
import net.exmo.exworld.mystery.GamePhase;
import net.exmo.exworld.mystery.MysteryGame;
import net.exmo.exworld.mystery.MysterySave;
import net.exmo.exworld.mystery.MysterySave.Participant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Traverses validated graphs. Every scheduled continuation retains the originating run id. */
public final class BlueprintRuntime {
    private BlueprintRuntime() {}

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(BlueprintRuntime::tick);
    }

    public static void emit(MinecraftServer server, String type, ServerPlayer player, String detail) {
        BlueprintStore store = BlueprintStore.get(server);
        MysterySave data = MysterySave.get(server);
        UUID run = data.runId;
        String eventKey = run + "/" + type + "/" + (player == null ? "global" : player.getUUID()) + "/" + detail;
        if (store.fired.putIfAbsent(eventKey, true) != null) return;
        store.changed();
        for (var entry : store.active.entrySet()) {
            if (!run.equals(data.runId)) break;
            BlueprintGraph graph;
            try { graph = BlueprintGraph.parse(entry.getValue()); }
            catch (RuntimeException error) { Exworld.LOGGER.error("Invalid active blueprint {}", entry.getKey(), error); continue; }
            for (BlueprintGraph.Node node : graph.nodes()) {
                if (!run.equals(data.runId)) break;
                if (!node.type().equals("trigger") || !node.text("kind").equals(type)) continue;
                String filter = node.text("value");
                if (!filter.isBlank() && !filter.equals(detail)) continue;
                for (BlueprintGraph.Node next : graph.next(node.id(), "next"))
                    execute(server, store, data, graph, next, player, detail, run, 0);
            }
        }
    }

    private static void execute(MinecraftServer server, BlueprintStore store, MysterySave data, BlueprintGraph graph,
                                BlueprintGraph.Node node, ServerPlayer player, String detail, UUID run, int depth) {
        if (node == null || depth > 256 || !run.equals(data.runId)) return;
        if (node.type().equals("condition")) {
            String port = condition(data, player, node) ? "true" : "false";
            for (BlueprintGraph.Node next : graph.next(node.id(), port))
                execute(server, store, data, graph, next, player, detail, run, depth + 1);
            return;
        }
        if (node.type().equals("wait")) {
            store.pending.add(new BlueprintStore.Pending(run, graph.id().toString(), node.id(),
                    player == null ? null : player.getUUID(), detail, data.tick + node.number("ticks", 1)));
            store.changed();
            return;
        }
        if (node.type().equals("cue")) {
            if (player != null) MysteryGame.cue(player, node.text("id"), node.number("duration", 80));
            else for (ServerPlayer online : server.getPlayerList().getPlayers())
                MysteryGame.cue(online, node.text("id"), node.number("duration", 80));
        } else if (node.type().equals("action")) {
            action(server, data, player, node);
        }
        for (BlueprintGraph.Node next : graph.next(node.id(), "next"))
            execute(server, store, data, graph, next, player, detail, run, depth + 1);
    }

    private static boolean condition(MysterySave data, ServerPlayer player, BlueprintGraph.Node node) {
        Participant p = player == null ? null : data.participant(player.getUUID());
        String value = node.text("value");
        return switch (node.text("kind")) {
            case "era" -> p != null && p.era.name().equalsIgnoreCase(value);
            case "state" -> p != null && p.state.name().equalsIgnoreCase(value);
            case "phase" -> data.phase.name().equalsIgnoreCase(value);
            case "character" -> p != null && p.characterId.equals(value);
            case "has_clue" -> p != null && p.knowledge.contains(value);
            case "seal_min" -> data.sealCount >= node.number("min", 0);
            case "cycle_min" -> data.cycle >= node.number("min", 0);
            default -> false;
        };
    }

    private static void action(MinecraftServer server, MysterySave data, ServerPlayer player, BlueprintGraph.Node node) {
        String value = node.text("value");
        switch (node.text("kind")) {
            case "add_clue" -> { if (player != null) MysteryGame.discover(player, value); }
            case "add_suspicion" -> {
                Participant p = player == null ? null : data.participant(player.getUUID());
                if (p != null) {
                    p.suspicion = Math.max(0, Math.min(3, p.suspicion + node.number("amount", 1)));
                    data.changed(); MysteryGame.sync(player);
                }
            }
            case "give_item" -> {
                if (player == null) return;
                ResourceLocation id = ResourceLocation.tryParse(value);
                Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
                if (item == Items.AIR) return;
                ItemStack stack = new ItemStack(item, Math.max(1, Math.min(64, node.number("amount", 1))));
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            }
            case "run_function" -> {
                ResourceLocation id = ResourceLocation.tryParse(value);
                if (id != null && id.getNamespace().equals("exworld")) server.getFunctions().get(id)
                        .ifPresent(function -> server.getFunctions().execute(function, server.createCommandSourceStack().withPermission(2)));
            }
            case "reset_timeline" -> MysteryGame.rewind(server);
            case "finish" -> MysteryGame.resolveEnding(server);
            default -> {}
        }
    }

    private static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        MysterySave data = MysterySave.get(server);
        if (data.phase == GamePhase.LOBBY) return;
        BlueprintStore store = BlueprintStore.get(server);
        List<BlueprintStore.Pending> ready = new ArrayList<>();
        store.pending.removeIf(item -> {
            if (!item.run().equals(data.runId)) return true;
            if (item.due() > data.tick) return false;
            ready.add(item); return true;
        });
        if (!ready.isEmpty()) store.changed();
        for (BlueprintStore.Pending pending : ready) {
            if (!pending.run().equals(data.runId)) break;
            String json = store.active.get(pending.graph());
            if (json == null) continue;
            try {
                BlueprintGraph graph = BlueprintGraph.parse(json);
                ServerPlayer player = pending.player() == null ? null : server.getPlayerList().getPlayer(pending.player());
                if (pending.player() != null && player == null) continue;
                for (BlueprintGraph.Node next : graph.next(pending.node(), "next"))
                    execute(server, store, data, graph, next, player, pending.detail(), pending.run(), 0);
            } catch (RuntimeException error) {
                Exworld.LOGGER.error("Failed to resume blueprint {}", pending.graph(), error);
            }
        }
    }
}
