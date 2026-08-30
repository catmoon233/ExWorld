package net.exmo.exworld.client.quest;

import java.util.*;
import net.exmo.exworld.network.QuestJournalActionPayload;
import net.exmo.exworld.progress.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client cache is presentation only. It receives snapshots and never calculates quest progress. */
public final class QuestClient {
    private static QuestJournalSnapshot snapshot = new QuestJournalSnapshot(List.of(),List.of(),List.of(),Map.of(),null);
    private static ResourceLocation navigation;
    private QuestClient() {}
    public static void install(QuestJournalSnapshot value) { snapshot=value; if(navigation==null) navigation=value.pinnedQuest(); if(Minecraft.getInstance().screen instanceof QuestJournalScreen screen) screen.rebuildWidgets(); }
    public static QuestJournalSnapshot snapshot(){return snapshot;}
    public static Optional<QuestSnapshot> quest(ResourceLocation id){return snapshot.quests().stream().filter(value->value.id().equals(id)).findFirst();}
    public static Optional<QuestSnapshot> navigation(){return navigation==null?Optional.empty():quest(navigation);}
    public static void request(){PacketDistributor.sendToServer(new QuestJournalActionPayload(QuestJournalActionPayload.Action.REQUEST,"","",null));}
    public static void open(){request(); if(!(Minecraft.getInstance().screen instanceof QuestJournalScreen))Minecraft.getInstance().setScreen(new QuestJournalScreen());}
    public static void pin(ResourceLocation id){PacketDistributor.sendToServer(new QuestJournalActionPayload(QuestJournalActionPayload.Action.PIN,id.toString(),"",null));}
    public static void navigate(ResourceLocation id){navigation=id;PacketDistributor.sendToServer(new QuestJournalActionPayload(QuestJournalActionPayload.Action.NAVIGATE,id.toString(),"",null));}
    public static void branch(ResourceLocation id,String node){PacketDistributor.sendToServer(new QuestJournalActionPayload(QuestJournalActionPayload.Action.BRANCH,id.toString(),node,null));}
    public static void claim(UUID id){PacketDistributor.sendToServer(new QuestJournalActionPayload(QuestJournalActionPayload.Action.CLAIM_MAIL,"","",id));}
}
