package net.exmo.lotm.phone;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.exmo.lotm.Lotm;

public final class PhonePayloads {
    private PhonePayloads() {}

    public record PhoneActionPayload(String json) implements CustomPacketPayload {
        public static final Type<PhoneActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Lotm.MODID, "phone_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PhoneActionPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.stringUtf8(65536), PhoneActionPayload::json, PhoneActionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PhoneStatePayload(String json) implements CustomPacketPayload {
        public static final Type<PhoneStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Lotm.MODID, "phone_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PhoneStatePayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.stringUtf8(65536), PhoneStatePayload::json, PhoneStatePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
