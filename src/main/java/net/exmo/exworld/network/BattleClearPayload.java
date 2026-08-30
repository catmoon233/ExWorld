package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BattleClearPayload(String outcome) implements CustomPacketPayload {
    public static final Type<BattleClearPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "battle_clear"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BattleClearPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeUtf(payload.outcome), buffer -> new BattleClearPayload(buffer.readUtf()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
