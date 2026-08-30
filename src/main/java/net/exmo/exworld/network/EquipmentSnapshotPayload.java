package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EquipmentSnapshotPayload(String slot1, String slot2) implements CustomPacketPayload {
    public static final Type<EquipmentSnapshotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "equipment_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            EquipmentSnapshotPayload::encode, EquipmentSnapshotPayload::decode);
    private static void encode(RegistryFriendlyByteBuf b, EquipmentSnapshotPayload p) { b.writeUtf(p.slot1); b.writeUtf(p.slot2); }
    private static EquipmentSnapshotPayload decode(RegistryFriendlyByteBuf b) { return new EquipmentSnapshotPayload(b.readUtf(), b.readUtf()); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
