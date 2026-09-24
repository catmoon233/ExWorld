package net.exmo.exworld.npc.network;

import net.exmo.exworld.Exworld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class NpcPayloads {
    private NpcPayloads() {}

    public record Client(String kind, CompoundTag tag) implements CustomPacketPayload {
        public static final Type<Client> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "npc_client"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Client> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> { buffer.writeUtf(payload.kind); buffer.writeNbt(payload.tag); },
                buffer -> new Client(buffer.readUtf(), buffer.readNbt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Server(String kind, CompoundTag tag) implements CustomPacketPayload {
        public static final Type<Server> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "npc_server"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Server> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> { buffer.writeUtf(payload.kind, 32); buffer.writeNbt(payload.tag); },
                buffer -> new Server(buffer.readUtf(), buffer.readNbt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
