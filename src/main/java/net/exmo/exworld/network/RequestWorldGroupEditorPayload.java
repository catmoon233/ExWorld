package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client request for the latest server-authoritative collaborative group-editor projection. */
public record RequestWorldGroupEditorPayload() implements CustomPacketPayload {
    public static final Type<RequestWorldGroupEditorPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "request_world_group_editor"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestWorldGroupEditorPayload> STREAM_CODEC =
            StreamCodec.unit(new RequestWorldGroupEditorPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
