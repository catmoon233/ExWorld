package net.exmo.exworld.subtitle;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SubtitlePayload(Component title, Component subtitle, int durationTicks, int color) implements CustomPacketPayload {
    public static final Type<SubtitlePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "subtitle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SubtitlePayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.TRUSTED_STREAM_CODEC, SubtitlePayload::title,
            ComponentSerialization.TRUSTED_STREAM_CODEC, SubtitlePayload::subtitle,
            ByteBufCodecs.VAR_INT, SubtitlePayload::durationTicks,
            ByteBufCodecs.INT, SubtitlePayload::color,
            SubtitlePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
