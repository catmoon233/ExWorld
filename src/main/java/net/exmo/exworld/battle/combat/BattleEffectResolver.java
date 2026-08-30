package net.exmo.exworld.battle.combat;

import net.exmo.exworld.battle.combatant.Combatant;
import net.exmo.exworld.battle.skill.SkillDefinition;

/** Seam between battle rules and the health/defence implementation used by the host game. */
public interface BattleEffectResolver {
    double damage(Combatant actor, Combatant target, SkillDefinition skill, double requested);
    double heal(Combatant actor, Combatant target, SkillDefinition skill, double requested);

    static BattleEffectResolver logical() {
        return new BattleEffectResolver() {
            @Override public double damage(Combatant actor, Combatant target, SkillDefinition skill, double requested) {
                float before = target.health(); target.damage((float) requested); return before - target.health();
            }
            @Override public double heal(Combatant actor, Combatant target, SkillDefinition skill, double requested) {
                float before = target.health(); target.heal((float) requested); return target.health() - before;
            }
        };
    }
}
