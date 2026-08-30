package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public record CardCollectionActionPayload(Action action,int deckSlot,UUID cardId,List<UUID> materials,String value) implements CustomPacketPayload{
    public enum Action{REQUEST,ADD_TO_DECK,REMOVE_FROM_DECK,SET_ACTIVE,RENAME,FUSE}
    public static final Type<CardCollectionActionPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID,"card_collection_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CardCollectionActionPayload> STREAM_CODEC=StreamCodec.of(CardCollectionActionPayload::encode,CardCollectionActionPayload::decode);
    public CardCollectionActionPayload{materials=List.copyOf(materials);}
    private static void encode(RegistryFriendlyByteBuf b,CardCollectionActionPayload p){b.writeEnum(p.action);b.writeVarInt(p.deckSlot);b.writeBoolean(p.cardId!=null);if(p.cardId!=null)b.writeUUID(p.cardId);b.writeVarInt(p.materials.size());p.materials.forEach(b::writeUUID);b.writeUtf(p.value==null?"":p.value);}
    private static CardCollectionActionPayload decode(RegistryFriendlyByteBuf b){Action a=b.readEnum(Action.class);int slot=b.readVarInt();UUID id=b.readBoolean()?b.readUUID():null;int n=b.readVarInt();List<UUID> m=new ArrayList<>();for(int i=0;i<n;i++)m.add(b.readUUID());return new CardCollectionActionPayload(a,slot,id,m,b.readUtf());}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
