package net.exmo.exworld.battle.api;

import net.exmo.exworld.battle.model.BattleCell;

import java.util.UUID;

/** Client and AI intent. Values produced by the command are never trusted as combat results. */
public sealed interface BattleCommand permits BattleCommand.Move, BattleCommand.UseSkill,
        BattleCommand.SetReady, BattleCommand.SetAutoBattle, BattleCommand.Escape,
        BattleCommand.SkipIntro, BattleCommand.SelectReward, BattleCommand.ConfirmResult,
        BattleCommand.UseItem, BattleCommand.SwitchWeapon {
    BattleId battleId();
    long expectedRevision();
    UUID commandId();
    UUID actorId();

    record Move(BattleId battleId, long expectedRevision, UUID commandId, UUID actorId, BattleCell destination)
            implements BattleCommand {}
    record UseSkill(BattleId battleId, long expectedRevision, UUID commandId, UUID actorId,
                    UUID cardInstanceId, BattleCell targetCell, UUID targetId) implements BattleCommand {}
    record SetReady(BattleId battleId, long expectedRevision, UUID commandId, UUID actorId, boolean ready)
            implements BattleCommand {}
    record SetAutoBattle(BattleId battleId, long expectedRevision, UUID commandId, UUID actorId, boolean enabled)
            implements BattleCommand {}
    record Escape(BattleId battleId, long expectedRevision, UUID commandId, UUID actorId)
            implements BattleCommand {}
    record SkipIntro(BattleId battleId, long expectedRevision, UUID commandId, UUID actorId)
            implements BattleCommand {}
    record SelectReward(BattleId battleId, long expectedRevision, UUID commandId, UUID actorId, String candidateId)
            implements BattleCommand {}
    record ConfirmResult(BattleId battleId, long expectedRevision, UUID commandId, UUID actorId)
            implements BattleCommand {}
    record UseItem(BattleId battleId, long expectedRevision, UUID commandId, UUID actorId,
                   int inventorySlot, String itemId, BattleCell targetCell) implements BattleCommand {}
    record SwitchWeapon(BattleId battleId, long expectedRevision, UUID commandId, UUID actorId,
                        int weaponSlot) implements BattleCommand {}
}
