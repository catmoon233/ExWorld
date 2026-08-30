package net.exmo.exworld.battle.api;

import net.exmo.exworld.battle.model.BattleCell;
import java.util.UUID;

/** Immutable presentation event consumed by battle logs, broadcasts and animation adapters. */
public record BattleEvent(long sequence, int round, Type type, UUID actorId, UUID targetId,
                          String actorName, String targetName, String skillId, String skillNameKey, double amount,
                          BattleCell fromCell, BattleCell toCell) {
    public BattleEvent(long sequence, int round, Type type, UUID actorId, UUID targetId,
                       String actorName, String targetName, String skillId, String skillNameKey, double amount) {
        this(sequence, round, type, actorId, targetId, actorName, targetName, skillId, skillNameKey, amount, null, null);
    }
    public enum Type { SKILL, DAMAGE, HEAL, MOVE, STATUS, DOWNED }
}
