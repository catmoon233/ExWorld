package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.battle.api.BattleId;
import net.exmo.exworld.battle.api.BattleCommand;
import net.exmo.exworld.battle.model.BattleCell;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record BattleIntentPayload(BattleId battleId, long revision, UUID commandId, UUID actorId, Kind kind,
                                  int x, int z, int floorY, UUID cardId, UUID targetId, boolean flag, String value,
                                  int inventorySlot) implements CustomPacketPayload {
    public BattleIntentPayload(BattleId battleId, long revision, UUID commandId, UUID actorId, Kind kind,
                               int x, int z, int floorY, UUID cardId, UUID targetId, boolean flag, String value) {
        this(battleId, revision, commandId, actorId, kind, x, z, floorY, cardId, targetId, flag, value, -1);
    }
    public enum Kind { MOVE, USE_SKILL, READY, AUTO_BATTLE, ESCAPE, SKIP_INTRO, SELECT_REWARD, CONFIRM_RESULT, USE_ITEM, SWITCH_WEAPON }
    public static final Type<BattleIntentPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "battle_intent"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BattleIntentPayload> STREAM_CODEC = StreamCodec.of(BattleIntentPayload::encode, BattleIntentPayload::decode);

    public BattleCommand command() {
        return switch (kind) {
            case MOVE -> new BattleCommand.Move(battleId, revision, commandId, actorId, new BattleCell(x, z, floorY));
            case USE_SKILL -> new BattleCommand.UseSkill(battleId, revision, commandId, actorId, cardId, new BattleCell(x, z, floorY), targetId);
            case READY -> new BattleCommand.SetReady(battleId, revision, commandId, actorId, flag);
            case AUTO_BATTLE -> new BattleCommand.SetAutoBattle(battleId, revision, commandId, actorId, flag);
            case ESCAPE -> new BattleCommand.Escape(battleId, revision, commandId, actorId);
            case SKIP_INTRO -> new BattleCommand.SkipIntro(battleId, revision, commandId, actorId);
            case SELECT_REWARD -> new BattleCommand.SelectReward(battleId, revision, commandId, actorId, value);
            case CONFIRM_RESULT -> new BattleCommand.ConfirmResult(battleId, revision, commandId, actorId);
            case USE_ITEM -> new BattleCommand.UseItem(battleId, revision, commandId, actorId, inventorySlot, value, new BattleCell(x, z, floorY));
            case SWITCH_WEAPON -> new BattleCommand.SwitchWeapon(battleId, revision, commandId, actorId, inventorySlot);
        };
    }
    private static void encode(RegistryFriendlyByteBuf buffer, BattleIntentPayload value) {
        buffer.writeUUID(value.battleId.value()); buffer.writeVarLong(value.revision); buffer.writeUUID(value.commandId); buffer.writeUUID(value.actorId);
        buffer.writeEnum(value.kind); buffer.writeVarInt(value.x); buffer.writeVarInt(value.z); buffer.writeVarInt(value.floorY);
        buffer.writeBoolean(value.cardId != null); if (value.cardId != null) buffer.writeUUID(value.cardId);
        buffer.writeBoolean(value.targetId != null); if (value.targetId != null) buffer.writeUUID(value.targetId); buffer.writeBoolean(value.flag);
        buffer.writeUtf(value.value == null ? "" : value.value); buffer.writeVarInt(value.inventorySlot);
    }
    private static BattleIntentPayload decode(RegistryFriendlyByteBuf buffer) {
        BattleId battle = new BattleId(buffer.readUUID()); long revision = buffer.readVarLong(); UUID command = buffer.readUUID(), actor = buffer.readUUID();
        Kind kind = buffer.readEnum(Kind.class); int x = buffer.readVarInt(), z = buffer.readVarInt(), y = buffer.readVarInt();
        UUID card = buffer.readBoolean() ? buffer.readUUID() : null; UUID target = buffer.readBoolean() ? buffer.readUUID() : null;
        return new BattleIntentPayload(battle, revision, command, actor, kind, x, z, y, card, target, buffer.readBoolean(), buffer.readUtf(), buffer.readVarInt());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
