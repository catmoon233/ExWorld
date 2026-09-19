package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record ShipEditorPayload(String currentId, byte[] template, List<String> ids, List<String> names) implements CustomPacketPayload {
    public static final Type<ShipEditorPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "ship_editor"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipEditorPayload> STREAM_CODEC = StreamCodec.of(ShipEditorPayload::encode, ShipEditorPayload::decode);
    public ShipEditorPayload {
        if (template.length > 1_048_576) throw new IllegalArgumentException("ship editor template is too large");
        if (ids.size() != names.size()) throw new IllegalArgumentException("ship list is ragged");
    }
    private static void encode(RegistryFriendlyByteBuf buffer, ShipEditorPayload payload) {
        buffer.writeUtf(payload.currentId);
        buffer.writeByteArray(payload.template);
        buffer.writeVarInt(payload.ids.size());
        for (int i = 0; i < payload.ids.size(); i++) {
            buffer.writeUtf(payload.ids.get(i));
            buffer.writeUtf(payload.names.get(i));
        }
    }
    private static ShipEditorPayload decode(RegistryFriendlyByteBuf buffer) {
        String current = buffer.readUtf();
        byte[] data = buffer.readByteArray(1_048_576);
        int count = buffer.readVarInt();
        List<String> ids = new ArrayList<>(count);
        List<String> names = new ArrayList<>(count);
        for (int i = 0; i < count; i++) { ids.add(buffer.readUtf()); names.add(buffer.readUtf()); }
        return new ShipEditorPayload(current, data, ids, names);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

record ShipSaveTemplatePayload(byte[] template) implements CustomPacketPayload {
    public static final Type<ShipSaveTemplatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "ship_save"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipSaveTemplatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeByteArray(payload.template),
            buffer -> new ShipSaveTemplatePayload(buffer.readByteArray(1_048_576)));
    public ShipSaveTemplatePayload { if (template.length > 1_048_576) throw new IllegalArgumentException("ship template is too large"); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

record ShipActionPayload(Action action, String id, int entityId, BlockPos a, BlockPos b) implements CustomPacketPayload {
    public enum Action { OPEN_EDITOR, CAPTURE, MATERIALIZE, DISASSEMBLE, SPAWN, OPEN_UPGRADE }
    public static final Type<ShipActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "ship_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeEnum(payload.action); buffer.writeUtf(payload.id); buffer.writeVarInt(payload.entityId);
                buffer.writeBlockPos(payload.a); buffer.writeBlockPos(payload.b);
            },
            buffer -> new ShipActionPayload(buffer.readEnum(Action.class), buffer.readUtf(), buffer.readVarInt(),
                    buffer.readBlockPos(), buffer.readBlockPos()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

record ShipInteractPayload(int entityId, int x, int y, int z) implements CustomPacketPayload {
    public static final Type<ShipInteractPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "ship_interact"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipInteractPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeVarInt(payload.entityId); buffer.writeVarInt(payload.x); buffer.writeVarInt(payload.y); buffer.writeVarInt(payload.z); },
            buffer -> new ShipInteractPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

record ShipDrivePayload(int entityId, int flags, float yaw) implements CustomPacketPayload {
    public static final Type<ShipDrivePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "ship_drive"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipDrivePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeVarInt(payload.entityId); buffer.writeVarInt(payload.flags); buffer.writeFloat(payload.yaw); },
            buffer -> new ShipDrivePayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

record ShipUpgradePayload(int entityId, String partId, String variantId) implements CustomPacketPayload {
    public static final Type<ShipUpgradePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "ship_upgrade"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipUpgradePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeVarInt(payload.entityId); buffer.writeUtf(payload.partId); buffer.writeUtf(payload.variantId); },
            buffer -> new ShipUpgradePayload(buffer.readVarInt(), buffer.readUtf(), buffer.readUtf()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

record ShipHullSyncPayload(int entityId, byte[] hull, String templateId, Map<String, String> selection, double speed) implements CustomPacketPayload {
    public static final Type<ShipHullSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "ship_hull"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipHullSyncPayload> STREAM_CODEC = StreamCodec.of(ShipHullSyncPayload::encode, ShipHullSyncPayload::decode);
    public ShipHullSyncPayload { if (hull.length > 1_048_576) throw new IllegalArgumentException("ship hull is too large"); }
    private static void encode(RegistryFriendlyByteBuf buffer, ShipHullSyncPayload payload) {
        buffer.writeVarInt(payload.entityId);
        buffer.writeByteArray(payload.hull);
        buffer.writeUtf(payload.templateId);
        buffer.writeVarInt(payload.selection.size());
        payload.selection.forEach((part, variant) -> { buffer.writeUtf(part); buffer.writeUtf(variant); });
        buffer.writeDouble(payload.speed);
    }
    private static ShipHullSyncPayload decode(RegistryFriendlyByteBuf buffer) {
        int id = buffer.readVarInt();
        byte[] hull = buffer.readByteArray(1_048_576);
        String template = buffer.readUtf();
        int count = buffer.readVarInt();
        Map<String, String> selection = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) selection.put(buffer.readUtf(), buffer.readUtf());
        return new ShipHullSyncPayload(id, hull, template, selection, buffer.readDouble());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

record ShipHullDeltaPayload(int entityId, int revision, int x, int y, int z, String block) implements CustomPacketPayload {
    public static final Type<ShipHullDeltaPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "ship_delta"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipHullDeltaPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.entityId); buffer.writeVarInt(payload.revision);
                buffer.writeVarInt(payload.x); buffer.writeVarInt(payload.y); buffer.writeVarInt(payload.z); buffer.writeUtf(payload.block);
            },
            buffer -> new ShipHullDeltaPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

record ShipToolOverlayPayload(BlockPos a, BlockPos b, boolean present) implements CustomPacketPayload {
    public static final Type<ShipToolOverlayPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "ship_overlay"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipToolOverlayPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeBlockPos(payload.a); buffer.writeBlockPos(payload.b); buffer.writeBoolean(payload.present); },
            buffer -> new ShipToolOverlayPayload(buffer.readBlockPos(), buffer.readBlockPos(), buffer.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

record ShipUpgradeScreenPayload(int entityId, byte[] template, List<String> variantIds, List<byte[]> variantHulls) implements CustomPacketPayload {
    public static final Type<ShipUpgradeScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "ship_upgrade_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipUpgradeScreenPayload> STREAM_CODEC = StreamCodec.of(ShipUpgradeScreenPayload::encode, ShipUpgradeScreenPayload::decode);
    public ShipUpgradeScreenPayload {
        if (template.length > 1_048_576) throw new IllegalArgumentException("upgrade template is too large");
        if (variantIds.size() != variantHulls.size()) throw new IllegalArgumentException("variant list is ragged");
    }
    private static void encode(RegistryFriendlyByteBuf buffer, ShipUpgradeScreenPayload payload) {
        buffer.writeVarInt(payload.entityId);
        buffer.writeByteArray(payload.template);
        buffer.writeVarInt(payload.variantIds.size());
        for (int i = 0; i < payload.variantIds.size(); i++) {
            buffer.writeUtf(payload.variantIds.get(i));
            buffer.writeByteArray(payload.variantHulls.get(i));
        }
    }
    private static ShipUpgradeScreenPayload decode(RegistryFriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        byte[] template = buffer.readByteArray(1_048_576);
        int count = buffer.readVarInt();
        List<String> ids = new ArrayList<>(count);
        List<byte[]> hulls = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(buffer.readUtf());
            hulls.add(buffer.readByteArray(1_048_576));
        }
        return new ShipUpgradeScreenPayload(entityId, template, ids, hulls);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
