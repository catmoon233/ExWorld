package net.exmo.exkeys.network;

import net.exmo.exkeys.server.ExKeysServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ExKeysNetwork {
    private ExKeysNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(ExKeysPayloads.PullPayload.TYPE, ExKeysPayloads.PullPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ExKeysServer.pull(player);
                }));
        registrar.playToServer(ExKeysPayloads.PushPayload.TYPE, ExKeysPayloads.PushPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ExKeysServer.push(player, payload.json());
                }));
        registrar.playToClient(ExKeysPayloads.PolicyPayload.TYPE, ExKeysPayloads.PolicyPayload.STREAM_CODEC,
                (payload, context) -> ExKeysClientHooks.receive(payload));
    }
}
