package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.battle.api.BattleId;
import net.exmo.exworld.dungeon.model.DungeonRunState;
import net.exmo.exworld.dungeon.model.DungeonSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Server-authoritative exploration state for the dungeon HUD and confirmation flow. */
public record DungeonSnapshotPayload(DungeonSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<DungeonSnapshotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "dungeon_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DungeonSnapshotPayload> STREAM_CODEC =
            StreamCodec.of(DungeonSnapshotPayload::encode, DungeonSnapshotPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, DungeonSnapshotPayload payload) {
        DungeonSnapshot value = payload.snapshot();
        buffer.writeBoolean(value != null);
        if (value == null) return;
        buffer.writeUUID(value.runId()); buffer.writeUtf(value.dungeonId()); buffer.writeEnum(value.state());
        buffer.writeLong(value.seed()); buffer.writeInt(value.slotX()); buffer.writeInt(value.slotZ());
        buffer.writeInt(value.roomIndex()); buffer.writeUtf(value.roomId()); buffer.writeBoolean(value.bossDefeated());
        buffer.writeVarInt(value.members().size()); value.members().forEach(buffer::writeUUID);
        buffer.writeVarInt(value.claimedTreasures().size()); value.claimedTreasures().forEach(buffer::writeUtf);
        buffer.writeBoolean(value.battleId() != null); if (value.battleId() != null) buffer.writeUUID(value.battleId().value());
    }

    private static DungeonSnapshotPayload decode(RegistryFriendlyByteBuf buffer) {
        try {
            return decodeStrict(buffer);
        } catch (RuntimeException ignored) {
            // Dungeon HUD state is recoverable. If an older client/server sends a
            // different layout, consume the payload and wait for the next sync
            // instead of disconnecting the player from the integrated server.
            buffer.skipBytes(buffer.readableBytes());
            return new DungeonSnapshotPayload(null);
        }
    }

    private static DungeonSnapshotPayload decodeStrict(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) return new DungeonSnapshotPayload(null);
        UUID runId = buffer.readUUID(); String dungeonId = buffer.readUtf(); DungeonRunState state = buffer.readEnum(DungeonRunState.class);
        long seed = buffer.readLong(); int slotX = buffer.readInt(); int slotZ = buffer.readInt(); int roomIndex = buffer.readInt();
        String roomId = buffer.readUtf(); boolean boss = buffer.readBoolean();
        Set<UUID> members = new LinkedHashSet<>(); for (int i = 0; i < buffer.readVarInt(); i++) members.add(buffer.readUUID());
        Set<String> claimed = new LinkedHashSet<>(); for (int i = 0; i < buffer.readVarInt(); i++) claimed.add(buffer.readUtf());
        // battleId was added after the first dungeon payload was shipped. A client may
        // still receive an old snapshot that ends immediately after claimedTreasures.
        BattleId battleId = buffer.readableBytes() > 0 && buffer.readBoolean()
                ? new BattleId(buffer.readUUID()) : null;
        return new DungeonSnapshotPayload(new DungeonSnapshot(runId, dungeonId, state, seed, slotX, slotZ, roomIndex, roomId, members, claimed, boss, battleId));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
