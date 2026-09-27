package net.exmo.exkeys.network;

import net.exmo.exkeys.ExKeys;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class ExKeysPayloads {
    private ExKeysPayloads() {}

    public record PullPayload() implements CustomPacketPayload {
        public static final Type<PullPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExKeys.MODID, "pull"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PullPayload> STREAM_CODEC = StreamCodec.unit(new PullPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PushPayload(String json) implements CustomPacketPayload {
        public static final Type<PushPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExKeys.MODID, "push"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PushPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(KeyLimit.MAX_JSON), PushPayload::json, PushPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PolicyPayload(String json, boolean editable) implements CustomPacketPayload {
        public static final Type<PolicyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExKeys.MODID, "policy"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PolicyPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(KeyLimit.MAX_JSON), PolicyPayload::json,
                ByteBufCodecs.BOOL, PolicyPayload::editable,
                PolicyPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static final class KeyLimit {
        public static final int MAX_JSON = 262144;

        private KeyLimit() {}
    }
}
