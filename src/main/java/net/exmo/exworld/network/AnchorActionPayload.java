package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AnchorActionPayload(String anchorId, Action action) implements CustomPacketPayload {
    public enum Action { TELEPORT, SELECT_RESPAWN }
    public static final Type<AnchorActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "anchor_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnchorActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeUtf(payload.anchorId); buffer.writeEnum(payload.action); },
            buffer -> new AnchorActionPayload(buffer.readUtf(), buffer.readEnum(Action.class)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
