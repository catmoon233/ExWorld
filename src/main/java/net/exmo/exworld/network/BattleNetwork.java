package net.exmo.exworld.network;

import net.exmo.exworld.battle.BattleSystem;
import net.exmo.exworld.battle.api.BattleSnapshot;
import net.exmo.exworld.client.battle.BattleClient;
import net.exmo.exworld.client.battle.screen.CardCollectionScreen;
import net.exmo.exworld.battle.card.CardCollectionSnapshot;
import net.exmo.exworld.client.party.PartyClient;
import net.exmo.exworld.client.dungeon.DungeonClient;
import net.exmo.exworld.client.equipment.EquipmentClient;
import net.exmo.exworld.equipment.PlayerEquipmentSavedData;
import net.exmo.exworld.battle.party.PartySnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class BattleNetwork {
    private BattleNetwork() {}
    public static void register(PayloadRegistrar registrar) {
        MonsterNetwork.register(registrar);
        registrar.playToClient(BattleSnapshotPayload.TYPE, BattleSnapshotPayload.STREAM_CODEC,
                (payload, context) -> BattleClient.install(payload.snapshot()));
        registrar.playToClient(BattleClearPayload.TYPE, BattleClearPayload.STREAM_CODEC,
                (payload, context) -> BattleClient.clear(payload.outcome()));
        registrar.playToClient(EquipmentSnapshotPayload.TYPE, EquipmentSnapshotPayload.STREAM_CODEC,
                (payload, context) -> EquipmentClient.install(payload));
        registrar.playToServer(BattleIntentPayload.TYPE, BattleIntentPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) BattleSystem.submit(player, payload.command());
        });
        registrar.playToServer(EquipmentActionPayload.TYPE, EquipmentActionPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) BattleSystem.equipmentAction(player, payload);
        });
        registrar.playToClient(CardCollectionPayload.TYPE, CardCollectionPayload.STREAM_CODEC,
                (payload, context) -> CardCollectionScreen.install(payload.snapshot()));
        registrar.playToClient(PartySnapshotPayload.TYPE, PartySnapshotPayload.STREAM_CODEC,
                (payload, context) -> PartyClient.install(payload.snapshot()));
        registrar.playToClient(DungeonSnapshotPayload.TYPE, DungeonSnapshotPayload.STREAM_CODEC,
                (payload, context) -> DungeonClient.install(payload.snapshot()));
        registrar.playToServer(CardCollectionActionPayload.TYPE, CardCollectionActionPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) BattleSystem.cardCollectionAction(player, payload);
        });
    }
    public static void sendSnapshot(ServerPlayer player, BattleSnapshot snapshot) {
        PacketDistributor.sendToPlayer(player, new BattleSnapshotPayload(snapshot.forViewer(player.getUUID())));
    }
    public static void sendEquipment(ServerPlayer player, PlayerEquipmentSavedData.Slots snapshot) {
        sendEquipment(player, snapshot.first(), snapshot.second());
    }
    public static void sendEquipment(ServerPlayer player, String slot1, String slot2) {
        PacketDistributor.sendToPlayer(player, new EquipmentSnapshotPayload(slot1 == null ? "" : slot1, slot2 == null ? "" : slot2));
    }
    public static void sendClear(ServerPlayer player, String outcome) { PacketDistributor.sendToPlayer(player, new BattleClearPayload(outcome)); }
    public static void sendCollection(ServerPlayer player, CardCollectionSnapshot snapshot) { PacketDistributor.sendToPlayer(player, new CardCollectionPayload(snapshot)); }
    public static void sendParty(ServerPlayer player, PartySnapshot snapshot) { PacketDistributor.sendToPlayer(player, new PartySnapshotPayload(snapshot)); }
    public static void sendDungeon(ServerPlayer player, net.exmo.exworld.dungeon.model.DungeonSnapshot snapshot) {
        PacketDistributor.sendToPlayer(player, new DungeonSnapshotPayload(snapshot));
    }
}
