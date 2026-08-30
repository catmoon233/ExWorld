package net.exmo.exworld.battle.skill;

import net.exmo.exworld.battle.BattleSession;
import net.exmo.exworld.battle.combatant.Combatant;
import net.exmo.exworld.battle.card.CardDefinition;
import net.exmo.exworld.battle.card.SkillCard;
import net.exmo.exworld.battle.model.BattleCell;

public record SkillUse(BattleSession session, Combatant actor, Combatant target, BattleCell targetCell,
                       SkillCard card, CardDefinition cardDefinition, SkillDefinition definition) {}
