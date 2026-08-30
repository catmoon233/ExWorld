package net.exmo.exworld.battle.weapon;

import net.exmo.exworld.battle.BattleSession;
import net.exmo.exworld.battle.combatant.Combatant;

public interface WeaponPassive {
    String id();
    void onAttackCard(BattleSession session, Combatant actor);

    /** Small presentation value for the weapon slot HUD; the engine owns its meaning. */
    default int progress(Combatant actor) { return 0; }
}
