package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.inventory.ItemFootprint;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InventoryPayloads {
    private InventoryPayloads() {}

    public record OpenBackpackPayload() implements CustomPacketPayload {
        public static final Type<OpenBackpackPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "open_backpack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenBackpackPayload> STREAM_CODEC =
                StreamCodec.unit(new OpenBackpackPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RotateBackpackPayload(int slot) implements CustomPacketPayload {
        public static final Type<RotateBackpackPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "rotate_backpack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RotateBackpackPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> buf.writeVarInt(p.slot),
                buf -> new RotateBackpackPayload(buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record FootprintRulesPayload(Map<String, String> rules) implements CustomPacketPayload {
        public static final Type<FootprintRulesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "footprint_rules"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FootprintRulesPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.rules.size());
                    p.rules.forEach((id, size) -> {
                        buf.writeUtf(id);
                        buf.writeUtf(size);
                    });
                },
                buf -> {
                    int n = buf.readVarInt();
                    Map<String, String> rules = new LinkedHashMap<>();
                    for (int i = 0; i < n; i++) rules.put(buf.readUtf(), buf.readUtf());
                    return new FootprintRulesPayload(rules);
                });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CreativeEditPayload(String itemId, String size, String quality, boolean save) implements CustomPacketPayload {
        public static final Type<CreativeEditPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "creative_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CreativeEditPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.itemId);
                    buf.writeUtf(p.size);
                    buf.writeUtf(p.quality);
                    buf.writeBoolean(p.save);
                },
                buf -> new CreativeEditPayload(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record RequestFootprintEditorPayload() implements CustomPacketPayload {
        public static final Type<RequestFootprintEditorPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "request_footprint_editor"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestFootprintEditorPayload> STREAM_CODEC =
                StreamCodec.unit(new RequestFootprintEditorPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record FootprintEditorPayload(Map<String, String> rules) implements CustomPacketPayload {
        public static final Type<FootprintEditorPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "footprint_editor"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FootprintEditorPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.rules.size());
                    p.rules.forEach((id, size) -> {
                        buf.writeUtf(id);
                        buf.writeUtf(size);
                    });
                },
                buf -> {
                    int n = buf.readVarInt();
                    Map<String, String> rules = new LinkedHashMap<>();
                    for (int i = 0; i < n; i++) rules.put(buf.readUtf(), buf.readUtf());
                    return new FootprintEditorPayload(rules);
                });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record FootprintEditPayload(String itemId, String size, boolean remove) implements CustomPacketPayload {
        public static final Type<FootprintEditPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "footprint_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FootprintEditPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.itemId);
                    buf.writeUtf(p.size);
                    buf.writeBoolean(p.remove);
                },
                buf -> new FootprintEditPayload(buf.readUtf(), buf.readUtf(), buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static Map<String, ItemFootprint> parse(Map<String, String> tokens) {
        Map<String, ItemFootprint> values = new LinkedHashMap<>();
        if (tokens == null) return values;
        tokens.forEach((id, size) -> values.put(id, ItemFootprint.parse(size)));
        return values;
    }
}
