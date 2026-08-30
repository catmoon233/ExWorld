package net.exmo.exworld.battle.skill;

import net.exmo.exworld.battle.BattleSession;
import net.exmo.exworld.battle.combatant.Combatant;

public interface SkillAdapter {
    SkillResult execute(SkillUse use);
    default boolean requiresMana(BattleSession session, Combatant actor, SkillDefinition definition) { return true; }
    /** Authoritative time until the action's cast/impact presentation has completed. */
    default int actionTicks(SkillUse use, SkillResult result) { return 8; }
    /** Lets a skill change cost and range from the actor's current weapon without a second card id. */
    default SkillDefinition resolveForActor(BattleSession session, Combatant actor, SkillDefinition definition) {
        return definition;
    }
}
