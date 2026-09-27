package net.exmo.lotm.story;

import net.exmo.lotm.Lotm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class StoryPayloads {
    private StoryPayloads() {}

    public record OpenLetterPayload(String text) implements CustomPacketPayload {
        public static final Type<OpenLetterPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Lotm.MODID, "open_letter"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenLetterPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, OpenLetterPayload::text, OpenLetterPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SendLetterPayload(String text) implements CustomPacketPayload {
        public static final Type<SendLetterPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Lotm.MODID, "send_letter"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SendLetterPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SendLetterPayload::text, SendLetterPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BuyPayload(String offer) implements CustomPacketPayload {
        public static final Type<BuyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Lotm.MODID, "story_buy"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BuyPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, BuyPayload::offer, BuyPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
