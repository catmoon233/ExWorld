package net.exmo.exworld.network;

import java.util.UUID;
import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client expresses choices only; server validates every action against the saved quest state. */
public record QuestJournalActionPayload(Action action,String questId,String value,UUID mailId) implements CustomPacketPayload {
    public enum Action { REQUEST, PIN, NAVIGATE, BRANCH, CLAIM_MAIL }
    public static final Type<QuestJournalActionPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID,"quest_journal_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,QuestJournalActionPayload> STREAM_CODEC=StreamCodec.of((b,p)->{b.writeEnum(p.action);b.writeUtf(p.questId);b.writeUtf(p.value);b.writeBoolean(p.mailId!=null);if(p.mailId!=null)b.writeUUID(p.mailId);},b->new QuestJournalActionPayload(b.readEnum(Action.class),b.readUtf(),b.readUtf(),b.readBoolean()?b.readUUID():null));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
