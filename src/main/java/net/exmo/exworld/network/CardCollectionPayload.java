package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.battle.card.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public record CardCollectionPayload(CardCollectionSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<CardCollectionPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID,"card_collection"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CardCollectionPayload> STREAM_CODEC=StreamCodec.of(CardCollectionPayload::encode,CardCollectionPayload::decode);
    private static void encode(RegistryFriendlyByteBuf b,CardCollectionPayload p){var s=p.snapshot;b.writeVarInt(s.gold());b.writeVarInt(s.activeDeck());b.writeVarInt(s.cards().size());for(var c:s.cards()){b.writeUUID(c.id());b.writeUtf(c.cardId());b.writeVarInt(c.star());}b.writeVarInt(s.decks().size());for(var d:s.decks()){b.writeVarInt(d.slot());b.writeUtf(d.name());b.writeVarInt(d.cardIds().size());d.cardIds().forEach(b::writeUUID);}b.writeVarInt(s.definitions().size());for(var d:s.definitions()){b.writeUtf(d.id());b.writeUtf(d.nameKey());b.writeUtf(d.descriptionKey());b.writeUtf(d.rarity());b.writeVarInt(d.tags().size());d.tags().forEach(b::writeUtf);b.writeVarInt(d.stars().size());for(var tier:d.stars()){b.writeVarInt(tier.manaCost());b.writeVarInt(tier.range());b.writeVarInt(tier.adapterParameters().size());tier.adapterParameters().forEach((k,v)->{b.writeUtf(k);b.writeDouble(v);});}}}
    private static CardCollectionPayload decode(RegistryFriendlyByteBuf b){int gold=b.readVarInt(),active=b.readVarInt(),n=b.readVarInt();List<OwnedCardInstance> cards=new ArrayList<>();for(int i=0;i<n;i++)cards.add(new OwnedCardInstance(b.readUUID(),b.readUtf(),b.readVarInt()));int dn=b.readVarInt();List<PlayerCardCollection.DeckView> decks=new ArrayList<>();for(int i=0;i<dn;i++){int slot=b.readVarInt();String name=b.readUtf();int cn=b.readVarInt();List<UUID> ids=new ArrayList<>();for(int j=0;j<cn;j++)ids.add(b.readUUID());decks.add(new PlayerCardCollection.DeckView(slot,name,ids));}int count=b.readVarInt();List<CardCollectionSnapshot.CardSummary> definitions=new ArrayList<>();for(int i=0;i<count;i++){String id=b.readUtf(),name=b.readUtf(),desc=b.readUtf(),rarity=b.readUtf();int tc=b.readVarInt();List<String> tags=new ArrayList<>();for(int j=0;j<tc;j++)tags.add(b.readUtf());int sc=b.readVarInt();List<CardDefinition.StarTier> stars=new ArrayList<>();for(int j=0;j<sc;j++){int mana=b.readVarInt(),range=b.readVarInt(),pc=b.readVarInt();Map<String,Double> params=new LinkedHashMap<>();for(int k=0;k<pc;k++)params.put(b.readUtf(),b.readDouble());stars.add(new CardDefinition.StarTier(mana,range,params));}definitions.add(new CardCollectionSnapshot.CardSummary(id,name,desc,rarity,tags,stars));}return new CardCollectionPayload(new CardCollectionSnapshot(gold,active,cards,decks,definitions));}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
