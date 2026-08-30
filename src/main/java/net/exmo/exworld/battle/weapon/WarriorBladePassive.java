package net.exmo.exworld.battle.weapon;

import net.exmo.exworld.battle.BattleSession;
import net.exmo.exworld.battle.combatant.Combatant;

/** Independent passive implementation carried by the Warrior's Blade item definition. */
public final class WarriorBladePassive implements WeaponPassive {
    @Override public String id() { return "warrior_blade"; }

    @Override public void onAttackCard(BattleSession session, Combatant actor) {
        actor.addAttackCardCount();
        if (actor.attackCardCount() % 2 == 0) session.addDashCard(actor, id());
    }

    @Override public int progress(Combatant actor) { return actor.attackCardCount() % 2; }
}
