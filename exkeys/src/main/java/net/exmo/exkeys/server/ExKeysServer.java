package net.exmo.exkeys.server;

import net.exmo.exkeys.KeyPolicy;
import net.exmo.exkeys.network.ExKeysPayloads;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ExKeysServer {
    private ExKeysServer() {}

    public static void starting(ServerStartingEvent event) {
        KeyPolicyStore.load();
    }

    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) pull(player);
    }

    public static void pull(ServerPlayer player) {
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, payload(player));
    }

    public static void push(ServerPlayer player, String json) {
        if (player == null) return;
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.translatable("exkeys.message.denied"));
            pull(player);
            return;
        }
        KeyPolicy.ParseResult parsed = KeyPolicy.parse(json, true);
        if (!parsed.ok()) {
            player.sendSystemMessage(Component.translatable("exkeys.message.invalid"));
            pull(player);
            return;
        }
        if (!KeyPolicyStore.replace(parsed.policy())) {
            player.sendSystemMessage(Component.translatable("exkeys.message.write_failed"));
            pull(player);
            return;
        }
        player.sendSystemMessage(Component.translatable("exkeys.message.saved"));
        var server = player.getServer();
        if (server == null) return;
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(online, payload(online));
        }
    }

    private static ExKeysPayloads.PolicyPayload payload(ServerPlayer player) {
        return new ExKeysPayloads.PolicyPayload(KeyPolicyStore.get().toJson(), player.hasPermissions(2));
    }
}
