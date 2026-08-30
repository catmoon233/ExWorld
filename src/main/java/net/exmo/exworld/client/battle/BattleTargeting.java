package net.exmo.exworld.client.battle;

import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.skill.SkillDefinition;

import java.util.Collection;
import java.util.UUID;

/** Converts a tactical grid click into an intent without depending on rendered entity hitboxes. */
public final class BattleTargeting {
    private BattleTargeting() {}

    public static Intent move(BattleCell clickedCell) {
        return new Intent(clickedCell, null);
    }

    public static Intent skill(BattleCell clickedCell, SkillDefinition.TargetType targetType, UUID actorId,
                               Collection<Occupant> occupants) {
        if (targetType == SkillDefinition.TargetType.SELF) {
            Occupant actor = occupants.stream().filter(occupant -> occupant.id().equals(actorId)).findFirst().orElse(null);
            return new Intent(actor == null ? clickedCell : actor.cell(), actorId);
        }
        if (targetType == SkillDefinition.TargetType.CELL) return new Intent(clickedCell, null);
        UUID targetId = occupants.stream().filter(occupant -> sameCell(occupant.cell(), clickedCell))
                .map(Occupant::id).findFirst().orElse(null);
        return new Intent(clickedCell, targetId);
    }

    public static boolean sameCell(BattleCell left, BattleCell right) {
        return left != null && right != null && left.x() == right.x() && left.z() == right.z();
    }

    public record Occupant(UUID id, BattleCell cell) {}
    public record Intent(BattleCell cell, UUID targetId) {}
}
