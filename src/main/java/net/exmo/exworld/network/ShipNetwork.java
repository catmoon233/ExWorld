package net.exmo.exworld.network;

import net.exmo.exworld.client.ship.ShipClient;
import net.exmo.exworld.ship.ShipSystem;
import net.exmo.exworld.ship.entity.ShipEntity;
import net.exmo.exworld.ship.storage.ShipNbtCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ShipNetwork {
    private ShipNetwork() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(ShipEditorPayload.TYPE, ShipEditorPayload.STREAM_CODEC,
                (payload, context) -> ShipClient.openEditor(payload.currentId(), payload.template(), payload.ids(), payload.names()));
        registrar.playToClient(ShipHullSyncPayload.TYPE, ShipHullSyncPayload.STREAM_CODEC,
                (payload, context) -> ShipClient.installHull(payload.entityId(), payload.hull(), payload.templateId(), payload.selection(), payload.speed()));
        registrar.playToClient(ShipHullDeltaPayload.TYPE, ShipHullDeltaPayload.STREAM_CODEC,
                (payload, context) -> ShipClient.applyDelta(payload.entityId(), payload.x(), payload.y(), payload.z(), payload.block()));
        registrar.playToClient(ShipToolOverlayPayload.TYPE, ShipToolOverlayPayload.STREAM_CODEC,
                (payload, context) -> ShipClient.overlay(payload.a(), payload.b(), payload.present()));
        registrar.playToClient(ShipUpgradeScreenPayload.TYPE, ShipUpgradeScreenPayload.STREAM_CODEC,
                (payload, context) -> ShipClient.openUpgrade(payload.entityId(), payload.template(), payload.variantIds(), payload.variantHulls()));
        registrar.playToServer(ShipSaveTemplatePayload.TYPE, ShipSaveTemplatePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) ShipSystem.saveTemplate(player, payload.template());
        });
        registrar.playToServer(ShipActionPayload.TYPE, ShipActionPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) ShipSystem.action(player, payload.action().name(), payload.id(), payload.entityId());
        });
        registrar.playToServer(ShipInteractPayload.TYPE, ShipInteractPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) ShipSystem.interact(player, payload.entityId(), payload.x(), payload.y(), payload.z());
        });
        registrar.playToServer(ShipDrivePayload.TYPE, ShipDrivePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) ShipSystem.drive(player, payload.entityId(), payload.flags(), payload.yaw());
        });
        registrar.playToServer(ShipUpgradePayload.TYPE, ShipUpgradePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) ShipSystem.upgrade(player, payload.entityId(), payload.partId(), payload.variantId());
        });
    }

    public static void sendEditor(ServerPlayer player, String currentId, byte[] template, java.util.List<String> ids, java.util.List<String> names) {
        PacketDistributor.sendToPlayer(player, new ShipEditorPayload(currentId, template, ids, names));
    }

    public static void sendOverlay(ServerPlayer player, BlockPos a, BlockPos b, boolean present) {
        PacketDistributor.sendToPlayer(player, new ShipToolOverlayPayload(a, b, present));
    }

    public static void sendHull(ServerPlayer player, ShipEntity ship) {
        PacketDistributor.sendToPlayer(player, hullPayload(ship));
    }

    public static void broadcastHull(ServerLevel level, ShipEntity ship) {
        PacketDistributor.sendToPlayersTrackingEntity(ship, hullPayload(ship));
    }

    public static void broadcastDelta(ServerLevel level, ShipEntity ship, int x, int y, int z, String block) {
        PacketDistributor.sendToPlayersTrackingEntity(ship,
                new ShipHullDeltaPayload(ship.getId(), ship.hull().revision(), x, y, z, block));
    }

    public static void sendUpgradeScreen(ServerPlayer player, int entityId, byte[] template, java.util.List<String> ids, java.util.List<byte[]> hulls) {
        PacketDistributor.sendToPlayer(player, new ShipUpgradeScreenPayload(entityId, template, ids, hulls));
    }

    public static void saveTemplate(byte[] template) {
        PacketDistributor.sendToServer(new ShipSaveTemplatePayload(template));
    }

    public static void action(String action, String id, int entityId) {
        PacketDistributor.sendToServer(new ShipActionPayload(ShipActionPayload.Action.valueOf(action), id, entityId, BlockPos.ZERO, BlockPos.ZERO));
    }

    public static void interact(int entityId, int x, int y, int z) {
        PacketDistributor.sendToServer(new ShipInteractPayload(entityId, x, y, z));
    }

    public static void drive(int entityId, int flags, float yaw) {
        PacketDistributor.sendToServer(new ShipDrivePayload(entityId, flags, yaw));
    }

    public static void upgrade(int entityId, String partId, String variantId) {
        PacketDistributor.sendToServer(new ShipUpgradePayload(entityId, partId, variantId));
    }

    private static ShipHullSyncPayload hullPayload(ShipEntity ship) {
        return new ShipHullSyncPayload(ship.getId(), ShipNbtCodec.encodeHull(ship.hull()), ship.templateId(),
                ship.selection().variantByPart(), ship.maxSpeed());
    }
}
