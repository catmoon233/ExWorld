package net.exmo.exworld.network;

import net.exmo.exworld.client.DecryptionClient;
import net.exmo.exworld.client.WorldMapClient;
import net.exmo.exworld.client.ClientChunkGroupState;
import net.exmo.exworld.world.WorldSystem;
import net.exmo.exworld.world.model.WorldSnapshot;
import net.exmo.exworld.subtitle.SubtitlePayload;
import net.exmo.exworld.subtitle.client.SubtitleHud;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class WorldNetwork {
    private WorldNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        BattleNetwork.register(registrar);
        QuestNetwork.register(registrar);
        ShipNetwork.register(registrar);
        net.exmo.exworld.inventory.InventoryNetwork.register(registrar);
        net.exmo.exworld.npc.network.NpcNetwork.register(registrar);
        registrar.playToServer(RequestWorldMapPayload.TYPE, RequestWorldMapPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) sendSnapshot(player, WorldSystem.snapshot(player));
        });
        registrar.playToServer(RequestWorldGroupEditorPayload.TYPE, RequestWorldGroupEditorPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) WorldSystem.openGroupEditor(player);
        });
        registrar.playToServer(TravelPayload.TYPE, TravelPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) WorldSystem.travel(player, payload.tileId());
        });
        registrar.playToServer(MapTeleportPayload.TYPE, MapTeleportPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) WorldSystem.creativeTeleport(player, payload.x(), payload.z());
        });
        registrar.playToServer(SaveWorldGroupEditPayload.TYPE, SaveWorldGroupEditPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) WorldSystem.saveGroupEdit(player, payload);
        });
        registrar.playToClient(WorldSnapshotPayload.TYPE, WorldSnapshotPayload.STREAM_CODEC,
                (payload, context) -> WorldMapClient.receive(payload.snapshot()));
        registrar.playToClient(WorldGroupEditorPayload.TYPE, WorldGroupEditorPayload.STREAM_CODEC,
                (payload, context) -> WorldMapClient.receiveGroupEditor(payload));
        registrar.playToClient(ActiveChunkGroupPayload.TYPE, ActiveChunkGroupPayload.STREAM_CODEC,
                (payload, context) -> ClientChunkGroupState.install(payload.shape(), payload.archipelago()));
        registrar.playToClient(AnchorSnapshotPayload.TYPE, AnchorSnapshotPayload.STREAM_CODEC,
                (payload, context) -> WorldMapClient.receiveAnchors(payload.snapshot()));
        registrar.playToServer(AnchorActionPayload.TYPE, AnchorActionPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.action() == AnchorActionPayload.Action.TELEPORT) WorldSystem.teleportToAnchor(player, payload.anchorId());
                else WorldSystem.selectRespawn(player, payload.anchorId());
            }
        });
        registrar.playToServer(RequestRespawnAnchorsPayload.TYPE, RequestRespawnAnchorsPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) WorldSystem.sendRespawnChoices(player);
        });
        registrar.playToClient(SubtitlePayload.TYPE, SubtitlePayload.STREAM_CODEC,
                (payload, context) -> SubtitleHud.enqueue(payload));
        registrar.playToClient(DecryptionModePayload.TYPE, DecryptionModePayload.STREAM_CODEC,
                (payload, context) -> DecryptionClient.apply(payload.enabled()));
    }

    public static void sendSnapshot(ServerPlayer player, WorldSnapshot snapshot) {
        PacketDistributor.sendToPlayer(player, new WorldSnapshotPayload(snapshot));
    }

    public static void sendGroupEditor(ServerPlayer player, WorldSnapshot snapshot, String error) {
        PacketDistributor.sendToPlayer(player, new WorldGroupEditorPayload(snapshot, error));
    }

    public static void sendAnchorSnapshot(ServerPlayer player, net.exmo.exworld.world.model.AnchorSnapshot snapshot) {
        PacketDistributor.sendToPlayer(player, new AnchorSnapshotPayload(snapshot));
    }

    public static void sendDecryptionMode(ServerPlayer player, boolean enabled) {
        PacketDistributor.sendToPlayer(player, new DecryptionModePayload(enabled));
    }

    public static void sendActiveChunkGroup(ServerPlayer player, net.exmo.exworld.world.model.ChunkGroupShape shape) {
        PacketDistributor.sendToPlayer(player, new ActiveChunkGroupPayload(shape,
                net.exmo.exworld.world.generation.ArchipelagoPresets.isArchipelago(player.serverLevel())));
    }
}
