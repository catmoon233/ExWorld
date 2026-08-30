package net.exmo.exworld.network;

import java.util.*;
import net.exmo.exworld.client.quest.QuestClient;
import net.exmo.exworld.progress.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class QuestNetwork {
    private QuestNetwork() {}
    public static void register(net.neoforged.neoforge.network.registration.PayloadRegistrar registrar) {
        registrar.playToClient(QuestJournalPayload.TYPE,QuestJournalPayload.STREAM_CODEC,(payload,context)->QuestClient.install(payload.snapshot()));
        registrar.playToServer(QuestJournalActionPayload.TYPE,QuestJournalActionPayload.STREAM_CODEC,(payload,context)->{if(!(context.player() instanceof ServerPlayer player)||net.exmo.exworld.battle.BattleSystem.isParticipating(player.getUUID()))return; try { ResourceLocation quest=payload.questId().isBlank()?null:ResourceLocation.parse(payload.questId()); switch(payload.action()){case REQUEST,NAVIGATE->{}case PIN->PlayerProgressSystem.quests().pin(player,quest);case BRANCH->PlayerProgressSystem.quests().chooseBranch(player,quest,payload.value());case CLAIM_MAIL->{if(payload.mailId()!=null)PlayerProgressSystem.rewards().claim(player,payload.mailId());}} send(player);}catch(IllegalArgumentException ignored){}});
    }
    public static QuestJournalSnapshot snapshot(ServerPlayer player) { QuestModule quests=PlayerProgressSystem.quests(); List<QuestJournalSnapshot.Mail> mail=quests.mailbox(player).stream().map(value->new QuestJournalSnapshot.Mail(value.id(),value.source(),value.subject(),value.deliveredAt().toEpochMilli(),value.read(),value.attachments())).toList(); return new QuestJournalSnapshot(quests.snapshot(player),quests.timeline(player),mail,PlayerProgressSystem.vault().balances(player.getServer(),player.getUUID()),quests.pinned(player)); }
    public static void send(ServerPlayer player) { PacketDistributor.sendToPlayer(player,new QuestJournalPayload(snapshot(player))); }
}
