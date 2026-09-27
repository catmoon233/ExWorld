package net.exmo.exworld.mystery;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MysteryNetwork {
    private MysteryNetwork() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(MysteryPayloads.Client.TYPE, MysteryPayloads.Client.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> net.exmo.exworld.mystery.client.MysteryClient.receive(payload)));
        registrar.playToServer(MysteryPayloads.Server.TYPE, MysteryPayloads.Server.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player)
                        context.enqueueWork(() -> MysteryGame.handle(player, payload));
                });
    }

    public static void send(ServerPlayer player, String kind, CompoundTag tag) {
        PacketDistributor.sendToPlayer(player, new MysteryPayloads.Client(kind, tag));
    }
}
