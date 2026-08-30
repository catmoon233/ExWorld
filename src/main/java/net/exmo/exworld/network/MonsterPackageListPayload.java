package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public record MonsterPackageListPayload(List<Entry> packages,List<String> enabled) implements CustomPacketPayload {
    public record Entry(String id,String displayName,String version,boolean valid,String error) {}
    public static final Type<MonsterPackageListPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID,"monster_packages"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MonsterPackageListPayload> STREAM_CODEC=StreamCodec.of(MonsterPackageListPayload::encode,MonsterPackageListPayload::decode);
    public MonsterPackageListPayload{packages=List.copyOf(packages);enabled=List.copyOf(enabled);}
    private static void encode(RegistryFriendlyByteBuf b,MonsterPackageListPayload p){b.writeVarInt(p.packages.size());for(Entry e:p.packages){b.writeUtf(e.id);b.writeUtf(e.displayName);b.writeUtf(e.version);b.writeBoolean(e.valid);b.writeUtf(e.error);}b.writeVarInt(p.enabled.size());p.enabled.forEach(b::writeUtf);}
    private static MonsterPackageListPayload decode(RegistryFriendlyByteBuf b){int n=b.readVarInt();if(n<0||n>512)throw new IllegalArgumentException("invalid package list");List<Entry> entries=new ArrayList<>();for(int i=0;i<n;i++)entries.add(new Entry(b.readUtf(),b.readUtf(),b.readUtf(),b.readBoolean(),b.readUtf()));int m=b.readVarInt();List<String> enabled=new ArrayList<>();for(int i=0;i<m;i++)enabled.add(b.readUtf());return new MonsterPackageListPayload(entries,enabled);}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
