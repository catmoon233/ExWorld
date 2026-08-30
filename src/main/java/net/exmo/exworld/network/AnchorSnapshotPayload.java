package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.world.model.AnchorSnapshot;
import net.exmo.exworld.world.model.TravelAnchor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record AnchorSnapshotPayload(AnchorSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<AnchorSnapshotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "anchor_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnchorSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            AnchorSnapshotPayload::encode, AnchorSnapshotPayload::decode);
    private static void encode(RegistryFriendlyByteBuf buffer, AnchorSnapshotPayload payload) {
        buffer.writeVarInt(payload.snapshot.anchors().size());
        for (TravelAnchor anchor : payload.snapshot.anchors()) {
            buffer.writeUtf(anchor.id()); buffer.writeUtf(anchor.name()); buffer.writeBlockPos(anchor.pos()); buffer.writeUtf(anchor.tileId());
        }
        buffer.writeUtf(payload.snapshot.currentAnchorId());
        buffer.writeVarLong(payload.snapshot.cooldownRemainingMs());
        buffer.writeBoolean(payload.snapshot.respawnMode());
    }
    private static AnchorSnapshotPayload decode(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<TravelAnchor> anchors = new ArrayList<>(count);
        for (int i = 0; i < count; i++) anchors.add(new TravelAnchor(buffer.readUtf(), buffer.readUtf(), buffer.readBlockPos(), buffer.readUtf()));
        return new AnchorSnapshotPayload(new AnchorSnapshot(anchors, buffer.readUtf(), buffer.readVarLong(), buffer.readBoolean()));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
