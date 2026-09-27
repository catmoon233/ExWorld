package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;

public final class CharacterPayloads {
    private CharacterPayloads() {}

    public static final int MAX_BYTES = 48_000;

    public record OpenCharacterPayload() implements CustomPacketPayload {
        public static final Type<OpenCharacterPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "open_character"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenCharacterPayload> STREAM_CODEC =
                StreamCodec.unit(new OpenCharacterPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CharacterIntroPayload(String text) implements CustomPacketPayload {
        public static final Type<CharacterIntroPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "character_intro"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CharacterIntroPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> write(buffer, payload.text),
                buffer -> new CharacterIntroPayload(read(buffer)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void write(RegistryFriendlyByteBuf buffer, String text) {
        byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("character intro exceeds " + MAX_BYTES + " bytes");
        }
        buffer.writeVarInt(bytes.length);
        buffer.writeBytes(bytes);
    }

    static String read(RegistryFriendlyByteBuf buffer) {
        int length = buffer.readVarInt();
        if (length < 0 || length > MAX_BYTES) {
            throw new IllegalArgumentException("character intro length " + length);
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
