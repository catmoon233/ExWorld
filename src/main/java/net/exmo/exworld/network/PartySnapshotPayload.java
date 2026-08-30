package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.battle.party.PartySnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public record PartySnapshotPayload(PartySnapshot snapshot) implements CustomPacketPayload {
    public static final Type<PartySnapshotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "party_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PartySnapshotPayload> STREAM_CODEC=StreamCodec.of(PartySnapshotPayload::encode,PartySnapshotPayload::decode);
    private static void encode(RegistryFriendlyByteBuf b,PartySnapshotPayload p){b.writeVarInt(p.snapshot.members().size());for(var m:p.snapshot.members()){b.writeUUID(m.id());b.writeUtf(m.name());b.writeBoolean(m.leader());b.writeBoolean(m.online());b.writeFloat(m.health());b.writeFloat(m.maxHealth());b.writeFloat(m.mana());b.writeFloat(m.maxMana());b.writeDouble(m.distance());}}
    private static PartySnapshotPayload decode(RegistryFriendlyByteBuf b){int count=b.readVarInt();List<PartySnapshot.Member> members=new ArrayList<>();for(int i=0;i<count;i++)members.add(new PartySnapshot.Member(b.readUUID(),b.readUtf(),b.readBoolean(),b.readBoolean(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readDouble()));return new PartySnapshotPayload(new PartySnapshot(members));}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
