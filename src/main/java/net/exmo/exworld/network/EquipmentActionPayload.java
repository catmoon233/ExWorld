package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EquipmentActionPayload(Action action, int weaponSlot, int inventorySlot) implements CustomPacketPayload {
    public enum Action { SET, CLEAR, REQUEST }
    public static final Type<EquipmentActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "equipment_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentActionPayload> STREAM_CODEC = StreamCodec.of(
            EquipmentActionPayload::encode, EquipmentActionPayload::decode);
    private static void encode(RegistryFriendlyByteBuf b, EquipmentActionPayload p) {
        b.writeEnum(p.action); b.writeVarInt(p.weaponSlot); b.writeVarInt(p.inventorySlot);
    }
    private static EquipmentActionPayload decode(RegistryFriendlyByteBuf b) {
        return new EquipmentActionPayload(b.readEnum(Action.class), b.readVarInt(), b.readVarInt());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
